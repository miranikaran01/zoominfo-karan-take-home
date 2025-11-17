# Speech-to-Text Service

A Spring Boot microservice that provides speech-to-text transcription capabilities using the Faster Whisper model. The service accepts audio files and returns real-time transcription results via Server-Sent Events (SSE).

## Features

- 🎤 **Audio Transcription**: Convert audio files to text using Faster Whisper models
- 📡 **Streaming Support**: Real-time transcription results via Server-Sent Events (SSE)
- 🐳 **Docker Support**: Containerized application with Docker Compose for local development
- ☁️ **AWS Deployment**: Infrastructure as Code using AWS CDK for ECS Fargate deployment
- 📚 **API Documentation**: Interactive Swagger/OpenAPI documentation
- 🏥 **Health Checks**: Spring Boot Actuator endpoints for monitoring
- 🔄 **Reactive Architecture**: Built with Spring WebFlux for non-blocking I/O

## Prerequisites

- **Java 21** or higher
- **Gradle 8.x** or higher (wrapper included)
- **Docker** and **Docker Compose** for local development
- **Node.js 22** and **npm** (for infrastructure deployment)
- **AWS CLI** configured (for deployment)

## Quick Start

### Local Development with Docker Compose

The easiest way to run the application locally is using the provided Gradle tasks:

```bash
# Build and start all services (app + faster-whisper-server)
./gradlew dockerComposeUp

# Stop and remove all containers
./gradlew dockerComposeDown
```

This will:
1. Build the Spring Boot application JAR
2. Start the Faster Whisper server container
3. Start the application container
4. Make the service available at `http://localhost:8080`

### Manual Docker Compose

Alternatively, you can use Docker Compose directly:

```bash
# Build and start services
docker-compose -f docker-compose-local.yaml up -d --build

# View logs
docker-compose -f docker-compose-local.yaml logs -f

# Stop services
docker-compose -f docker-compose-local.yaml down
```

## API Documentation

### Swagger UI

Once the application is running, access the interactive API documentation [here](http://speech-speec-k3qudbyttljw-985865704.us-east-1.elb.amazonaws.com/api/v1/swagger-ui/index.html)

The Swagger UI provides:
- Complete API endpoint documentation
- Interactive request/response examples
- Try-it-out functionality for testing endpoints

### API Endpoints

#### POST `/speech-to-text`

Transcribes an audio file to text using the Faster Whisper model.

**Request:**
- **Content-Type**: `multipart/form-data`
- **Response**: `text/event-stream` (Server-Sent Events)

**Parameters:**
- `file` (required): Audio file to transcribe (max 100MB)
- `language` (optional): Language code (e.g., "en", "es", "fr")
- `model` (optional): Faster Whisper model to use (default: "Systran/faster-whisper-small")
- `stream` (optional): Whether to stream results (default: false)

**Example using cURL:**
```bash
curl -X POST http://localhost:8080/speech-to-text \
  -F "file=@audio.wav" \
  -F "language=en" \
  -F "model=Systran/faster-whisper-small" \
  -F "stream=true"
```

**Note**: This service has been tested with the [Four Max Carrados Detective Stories MP3 file](https://archive.org/download/carrados_librivox/four_max_carrados_detective_stories_04_bramah.mp3) from Archive.org. See the [Testing](#testing) section for more details.

## Health Checks

The application exposes health check endpoints via Spring Boot Actuator:

### Health Endpoint

```
GET http://localhost:8080/management/health
```

**Response:**
```json
{
  "status": "UP"
}
```
## Architecture

### Components

1. **Spring Boot Application** (`karan-take-home`)
   - REST API for receiving audio files
   - Reactive WebFlux client for Faster Whisper communication
   - Server-Sent Events for streaming responses

2. **Faster Whisper Server** (`faster-whisper-server`)
   - ML model server for speech transcription
   - Runs as a separate container/service
   - Uses the [faster-whisper-server](https://github.com/etalab-ia/faster-whisper-server) project, an OpenAI API-compatible transcription server

### Local Development Architecture

```
┌─────────────────────┐
│   Docker Compose    │
│                     │
│  ┌───────────────┐  │
│  │ karan-take-   │  │
│  │ home:8080     │  │
│  └───────┬───────┘  │
│          │          │
│          │ HTTP     │
│          │          │
│  ┌───────▼───────┐  │
│  │ faster-       │  │
│  │ whisper:8000  │  │
│  └───────────────┘  │
└─────────────────────┘
```

### Production Architecture (AWS ECS)

The application is deployed on AWS ECS Fargate with:
- **Application Load Balancer** for public access
- **ECS Fargate** service running the Spring Boot application
- **Sidecar container** running Faster Whisper server
- **VPC** with 2 availability zones for high availability
- **ECR** for container image storage

## Building

### Build the Application

```bash
# Build JAR file
./gradlew build

# Run tests
./gradlew test

# Build without tests
./gradlew build -x test
```

### Build Docker Image

```bash
# Build the application first
./gradlew build

# Build Docker image
docker build -t karan-take-home:latest .
```

## Testing

Run the test suite:

```bash
./gradlew test
```

Test configuration is in `src/test/resources/application-test.properties` with adjusted file size limits for testing.

### Tested Audio Files

This application has been tested with the following audio file:

- **MP3 Audio File**: [Four Max Carrados Detective Stories - The Last Exploit of Harry the Actor](https://archive.org/download/carrados_librivox/four_max_carrados_detective_stories_04_bramah.mp3)
  - Source: Archive.org (LibriVox)
  - Format: MP3
  - Description: Audiobook chapter from "Four Max Carrados Detective Stories" by Ernest Bramah

You can download and test the service with this file using:

```bash
# Download the test file
curl -O https://archive.org/download/carrados_librivox/four_max_carrados_detective_stories_04_bramah.mp3

# Test transcription
curl -X POST http://localhost:8080/speech-to-text \
  -F "file=@four_max_carrados_detective_stories_04_bramah.mp3" \
  -F "language=en" \
  -F "stream=true" 
```

## Deployment

### AWS Deployment

The project includes AWS CDK infrastructure for deploying to ECS Fargate.

#### Prerequisites

1. AWS CLI configured with appropriate credentials
2. CDK bootstrapped in your AWS account:
   ```bash
   cd infra
   npm install
   npm run bootstrap
   ```

#### Deploy via GitHub Actions

The project includes a GitHub Actions workflow (`.github/workflows/deploy.yml`) that:
1. Builds the application
2. Builds and pushes Docker image to ECR
3. Deploys the CDK stack to AWS

**Required GitHub Environment Variables:**
- `AWS_REGION`: AWS region (e.g., `us-east-1`)
- `AWS_ACCOUNT_ID`: AWS account ID

**Required AWS Resources:**
- ECR repository named `speech-to-text`
- IAM role `SpeechToTextGitHubActions` for OIDC authentication

#### Manual Deployment

```bash
# Build and push Docker image
./gradlew build
docker build -t karan-take-home:latest .
# Tag and push to ECR...

# Deploy infrastructure
cd infra
npm install
npm run build
npm run deploy
```
## Credits

This project uses the following open-source components:

- **[faster-whisper-server](https://github.com/etalab-ia/faster-whisper-server)**: An OpenAI API-compatible transcription server that uses faster-whisper as its backend. The Docker image `fedirz/faster-whisper-server:sha-307e23f-cpu` is used for speech-to-text transcription.
