package config

import "os"

type Config struct {
	PulsarURL string
	DBHost    string
	DBPort    string
	DBUser    string
	DBPass    string
	DBName    string
	HTTPPort  string

	// Cron expressions
	DailyCron     string
	WeeklyCron    string
	QuarterlyCron string
}

func Load() *Config {
	return &Config{
		PulsarURL: getEnv("PULSAR_URL", "pulsar://localhost:6650"),
		DBHost:    getEnv("DB_HOST", "localhost"),
		DBPort:    getEnv("DB_PORT", "5432"),
		DBUser:    getEnv("DB_USER", "stockpicker"),
		DBPass:    getEnv("DB_PASS", "stockpicker"),
		DBName:    getEnv("DB_NAME", "stockpicker"),
		HTTPPort:  getEnv("HTTP_PORT", "8083"),

		// Default: Daily at 18:00 IST (12:30 UTC), Weekly Saturday 06:00 IST, Quarterly
		DailyCron:     getEnv("DAILY_CRON", "30 12 * * 1-5"),
		WeeklyCron:    getEnv("WEEKLY_CRON", "30 0 * * 6"),
		QuarterlyCron: getEnv("QUARTERLY_CRON", "0 6 1 1,4,7,10 *"),
	}
}

func (c *Config) DSN() string {
	return "host=" + c.DBHost +
		" port=" + c.DBPort +
		" user=" + c.DBUser +
		" password=" + c.DBPass +
		" dbname=" + c.DBName +
		" sslmode=disable"
}

func getEnv(key, fallback string) string {
	if val := os.Getenv(key); val != "" {
		return val
	}
	return fallback
}
