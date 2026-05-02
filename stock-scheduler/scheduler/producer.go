package scheduler

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"time"

	"github.com/apache/pulsar-client-go/pulsar"
)

const TopicName = "persistent://public/default/stock-tasks"

type TaskMessage struct {
	Ticker     string `json:"ticker"`
	TaskType   string `json:"taskType"`
	TargetDate string `json:"targetDate,omitempty"`
}

type Producer struct {
	client   pulsar.Client
	producer pulsar.Producer
}

func NewProducer(pulsarURL string) (*Producer, error) {
	client, err := pulsar.NewClient(pulsar.ClientOptions{
		URL:               pulsarURL,
		OperationTimeout:  30 * time.Second,
		ConnectionTimeout: 30 * time.Second,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to create Pulsar client: %w", err)
	}

	producer, err := client.CreateProducer(pulsar.ProducerOptions{
		Topic: TopicName,
	})
	if err != nil {
		client.Close()
		return nil, fmt.Errorf("failed to create Pulsar producer: %w", err)
	}

	log.Printf("Pulsar producer connected to %s on topic %s", pulsarURL, TopicName)
	return &Producer{client: client, producer: producer}, nil
}

func (p *Producer) SendTask(ticker string, taskType string) error {
	msg := TaskMessage{
		Ticker:     ticker,
		TaskType:   taskType,
		TargetDate: time.Now().Format("2006-01-02"),
	}

	payload, err := json.Marshal(msg)
	if err != nil {
		return fmt.Errorf("failed to marshal task message: %w", err)
	}

	_, err = p.producer.Send(context.Background(), &pulsar.ProducerMessage{
		Payload: payload,
	})
	if err != nil {
		return fmt.Errorf("failed to send message for %s: %w", ticker, err)
	}

	log.Printf("Sent task: %s for ticker: %s", taskType, ticker)
	return nil
}

func (p *Producer) Close() {
	p.producer.Close()
	p.client.Close()
	log.Println("Pulsar producer closed")
}
