package scheduler

import (
	"database/sql"
	"log"

	"github.com/robfig/cron/v3"
)

type Scheduler struct {
	cron     *cron.Cron
	db       *sql.DB
	producer *Producer
}

func New(db *sql.DB, producer *Producer) *Scheduler {
	return &Scheduler{
		cron:     cron.New(),
		db:       db,
		producer: producer,
	}
}

func (s *Scheduler) SetupJobs(dailyCron, weeklyCron, quarterlyCron string) error {
	// Daily job: fetch price data for all tickers (Mon-Fri 18:00 IST)
	_, err := s.cron.AddFunc(dailyCron, func() {
		log.Println("[CRON] Running daily price fetch...")
		s.publishForAllTickers("FETCH_PRICE_DAILY")
	})
	if err != nil {
		return err
	}

	// Weekly job: full backfill (Saturday)
	_, err = s.cron.AddFunc(weeklyCron, func() {
		log.Println("[CRON] Running weekly backfill...")
		s.publishForAllTickers("FETCH_PRICE_DAILY")
	})
	if err != nil {
		return err
	}

	// Quarterly job: fetch financials
	_, err = s.cron.AddFunc(quarterlyCron, func() {
		log.Println("[CRON] Running quarterly financials fetch...")
		s.publishForAllTickers("FETCH_FINANCIALS_QUARTERLY")
	})
	if err != nil {
		return err
	}

	log.Printf("Scheduled jobs: daily=%s, weekly=%s, quarterly=%s",
		dailyCron, weeklyCron, quarterlyCron)
	return nil
}

func (s *Scheduler) Start() {
	s.cron.Start()
	log.Println("Cron scheduler started")
}

func (s *Scheduler) Stop() {
	s.cron.Stop()
	log.Println("Cron scheduler stopped")
}

func (s *Scheduler) Entries() []cron.Entry {
	return s.cron.Entries()
}

// TriggerFullRefresh triggers all task types for a single ticker
func (s *Scheduler) TriggerFullRefresh(ticker string) error {
	taskTypes := []string{
		"FETCH_PRICE_DAILY",
		"FETCH_FINANCIALS_QUARTERLY",
		"SCRAPE_NEWS_SENTIMENT",
	}
	for _, t := range taskTypes {
		if err := s.producer.SendTask(ticker, t); err != nil {
			return err
		}
	}
	return nil
}

// TriggerAll triggers a specific task type for all tickers
func (s *Scheduler) TriggerAll(taskType string) {
	s.publishForAllTickers(taskType)
}

func (s *Scheduler) publishForAllTickers(taskType string) {
	tickers, err := s.fetchAllTickers()
	if err != nil {
		log.Printf("Error fetching tickers: %v", err)
		return
	}

	log.Printf("Publishing %s for %d tickers", taskType, len(tickers))
	for _, ticker := range tickers {
		if err := s.producer.SendTask(ticker, taskType); err != nil {
			log.Printf("Error sending task for %s: %v", ticker, err)
		}
	}
}

func (s *Scheduler) fetchAllTickers() ([]string, error) {
	rows, err := s.db.Query("SELECT ticker FROM stocks ORDER BY ticker")
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var tickers []string
	for rows.Next() {
		var ticker string
		if err := rows.Scan(&ticker); err != nil {
			return nil, err
		}
		tickers = append(tickers, ticker)
	}
	return tickers, rows.Err()
}
