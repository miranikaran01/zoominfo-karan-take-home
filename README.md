# Speech-to-Text Service

A production-ready Spring Boot microservice that provides speech-to-text transcription capabilities using the Faster Whisper model. The service accepts audio files and returns real-time transcription results via Server-Sent Events (SSE).

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

### Running Without Docker

If you prefer to run the application directly:

1. **Start the Faster Whisper server**:
   ```bash
   docker run -d -p 8000:8000 --name faster-whisper \
     fedirz/faster-whisper-server:sha-307e23f-cpu
   ```

2. **Set environment variable**:
   ```bash
   export WHISPER_URL=http://localhost:8000
   ```

3. **Build and run the application**:
   ```bash
   ./gradlew build
   java -jar build/libs/karan-take-home-0.0.1-SNAPSHOT.jar
   ```

The application will be available at `http://localhost:8080`.

## API Documentation

### Swagger UI

Once the application is running, access the interactive API documentation at:

```
http://localhost:8080/api/v1/docs
```

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
  -F "stream=true" \
  --no-buffer
```

**Note**: This service has been tested with the [Four Max Carrados Detective Stories MP3 file](https://archive.org/download/carrados_librivox/four_max_carrados_detective_stories_04_bramah.mp3) from Archive.org. See the [Testing](#testing) section for more details.

**Example using Swagger UI:**
Navigate to `http://localhost:8080/api/v1/docs` and use the interactive interface.

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

### Additional Actuator Endpoints

All actuator endpoints are available under the `/management` base path:
- `/management/health` - Application health status
- `/management/info` - Application information (if configured)

## Configuration

### Application Properties

Key configuration options in `src/main/resources/application.properties`:

```properties
# Application name
spring.application.name=karan-take-home

# File upload limits
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=105MB
server.tomcat.max-http-post-size=115MB
server.tomcat.max-swallow-size=110MB

# Faster Whisper server URL
faster.whisper.url=${WHISPER_URL:http://faster-whisper-server:8000}

# API Documentation
springdoc.swagger-ui.path=/api/v1/docs

# Actuator
management.endpoints.web.base-path=/management
```

### Environment Variables

- `WHISPER_URL`: URL of the Faster Whisper server (default: `http://faster-whisper-server:8000`)
  - For local development: `http://localhost:8000`
  - For Docker Compose: `http://faster-whisper-server:8000`
  - For ECS sidecar: `http://localhost:8000`

## Architecture

### Components

1. **Spring Boot Application** (`karan-take-home`)
   - REST API for receiving audio files
   - Reactive WebFlux client for Faster Whisper communication
   - Server-Sent Events for streaming responses

2. **Faster Whisper Server** (`faster-whisper-server`)
   - ML model server for speech transcription
   - Runs as a separate container/service

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
  -F "stream=true" \
  --no-buffer
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

## Project Structure

```
.
├── src/
│   ├── main/
│   │   ├── java/com/zoominfo/karan_take_home/
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── services/            # Business logic
│   │   │   ├── clients/             # External service clients
│   │   │   ├── config/              # Configuration classes
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   └── interceptors/        # WebClient interceptors
│   │   └── resources/
│   │       └── application.properties
│   └── test/                        # Test files
├── infra/                           # AWS CDK infrastructure
│   ├── bin/app.ts                   # CDK app entry point
│   ├── lib/ecs-stack.ts             # ECS stack definition
│   └── package.json
├── docker-compose-local.yaml        # Local development setup
├── Dockerfile                       # Application container
├── build.gradle                     # Build configuration
└── README.md                        # This file
```

## Development

### Adding Dependencies

Add dependencies in `build.gradle`:

```gradle
dependencies {
    implementation 'group:artifact:version'
}
```

### Code Style

The project uses:
- **Lombok** for reducing boilerplate code
- **Spring Boot** best practices
- **Reactive programming** with Project Reactor

## Troubleshooting

### Application can't connect to Faster Whisper server

- **Docker Compose**: Ensure `WHISPER_URL=http://faster-whisper-server:8000` is set
- **Local**: Ensure `WHISPER_URL=http://localhost:8000` is set
- Check that the Faster Whisper container is running: `docker ps`

### File upload size errors

- Check `application.properties` for file size limits
- Ensure all size limits are properly configured (file, request, post, swallow)

### Port conflicts

- Application runs on port `8080` by default
- Faster Whisper server runs on port `8000`
- Modify ports in `docker-compose-local.yaml` if needed

## License

This project is part of a take-home assessment.

## Contributing

This is a take-home project. For questions or issues, please contact the repository maintainer.

