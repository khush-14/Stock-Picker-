package main

import (
	"database/sql"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/gin-gonic/gin"
	_ "github.com/lib/pq"
	"github.com/stockpicker/stock-scheduler/config"
	"github.com/stockpicker/stock-scheduler/handlers"
	"github.com/stockpicker/stock-scheduler/scheduler"
)

func main() {
	log.Println("Starting Stock Scheduler Service...")
	cfg := config.Load()

	// ── Database Connection ──
	db, err := sql.Open("postgres", cfg.DSN())
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	defer db.Close()

	if err := db.Ping(); err != nil {
		log.Fatalf("Database ping failed: %v", err)
	}
	log.Println("Database connected")

	// ── Pulsar Producer ──
	producer, err := scheduler.NewProducer(cfg.PulsarURL)
	if err != nil {
		log.Fatalf("Failed to create Pulsar producer: %v", err)
	}
	defer producer.Close()

	// ── Cron Scheduler ──
	sched := scheduler.New(db, producer)
	if err := sched.SetupJobs(cfg.DailyCron, cfg.WeeklyCron, cfg.QuarterlyCron); err != nil {
		log.Fatalf("Failed to setup cron jobs: %v", err)
	}
	sched.Start()
	defer sched.Stop()

	// ── HTTP Server ──
	gin.SetMode(gin.ReleaseMode)
	router := gin.Default()

	router.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{"status": "healthy", "service": "stock-scheduler"})
	})

	handlers.RegisterRoutes(router, sched)

	// ── Graceful Shutdown ──
	go func() {
		addr := fmt.Sprintf(":%s", cfg.HTTPPort)
		log.Printf("HTTP server listening on %s", addr)
		if err := router.Run(addr); err != nil {
			log.Fatalf("HTTP server failed: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	log.Println("Shutting down Stock Scheduler Service...")
}
