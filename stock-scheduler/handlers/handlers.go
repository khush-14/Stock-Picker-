package handlers

import (
	"fmt"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/stockpicker/stock-scheduler/scheduler"
)

func RegisterRoutes(r *gin.Engine, s *scheduler.Scheduler) {
	api := r.Group("/api/v1/scheduler")
	{
		api.GET("/status", statusHandler(s))
		api.POST("/trigger/:ticker", triggerTickerHandler(s))
		api.POST("/trigger-all", triggerAllHandler(s))
	}
}

func statusHandler(s *scheduler.Scheduler) gin.HandlerFunc {
	return func(c *gin.Context) {
		entries := s.Entries()
		jobs := make([]map[string]string, 0, len(entries))

		for _, entry := range entries {
			jobs = append(jobs, map[string]string{
				"id":       fmt.Sprintf("%d", entry.ID),
				"nextRun":  entry.Next.Format("2006-01-02 15:04:05 MST"),
				"lastRun":  entry.Prev.Format("2006-01-02 15:04:05 MST"),
			})
		}

		c.JSON(http.StatusOK, gin.H{
			"status": "running",
			"jobs":   jobs,
		})
	}
}

func triggerTickerHandler(s *scheduler.Scheduler) gin.HandlerFunc {
	return func(c *gin.Context) {
		ticker := c.Param("ticker")
		if ticker == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "ticker is required"})
			return
		}

		if err := s.TriggerFullRefresh(ticker); err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{
				"error": fmt.Sprintf("failed to trigger refresh: %v", err),
			})
			return
		}

		c.JSON(http.StatusOK, gin.H{
			"status":  "triggered",
			"ticker":  ticker,
			"message": "Full refresh triggered for all task types",
		})
	}
}

func triggerAllHandler(s *scheduler.Scheduler) gin.HandlerFunc {
	return func(c *gin.Context) {
		taskType := c.DefaultQuery("taskType", "FETCH_PRICE_DAILY")
		go s.TriggerAll(taskType)

		c.JSON(http.StatusAccepted, gin.H{
			"status":   "accepted",
			"taskType": taskType,
			"message":  "Trigger-all job submitted in background",
		})
	}
}
