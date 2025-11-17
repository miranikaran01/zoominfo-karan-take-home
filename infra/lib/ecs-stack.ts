import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ecsPatterns from 'aws-cdk-lib/aws-ecs-patterns';
import * as ecr from 'aws-cdk-lib/aws-ecr';

export class EcsStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // IMAGE_TAG comes from GitHub Actions; fall back to 'latest' for local dev
    const imageTag = process.env.IMAGE_TAG ?? 'latest';

    const vpc = new ec2.Vpc(this, 'speech-to-text-vpc', {
      maxAzs: 2
    });

    const cluster = new ecs.Cluster(this, 'speech-to-text-cluster', {
      vpc
    });

    // ECR repo for speech-to-text app
    const appRepo = ecr.Repository.fromRepositoryName(
      this,
      'speech-to-text-app-repo',
      'speech-to-text'
    );

    // Fargate service with ALB 
    const albFargate = new ecsPatterns.ApplicationLoadBalancedFargateService(
      this,
      'speech-to-text-service',
      {
        cluster,
        desiredCount: 1,
        cpu: 2048,           // 2 vCPU
        memoryLimitMiB: 4096, // 4 GB – give Whisper some room
        publicLoadBalancer: true,
        runtimePlatform: {
          cpuArchitecture: ecs.CpuArchitecture.ARM64,
          operatingSystemFamily: ecs.OperatingSystemFamily.LINUX
        },
        taskImageOptions: {
          containerName: 'speech-to-text',
          image: ecs.ContainerImage.fromEcrRepository(appRepo, imageTag),
          containerPort: 8080,
          environment: {
            // Point app at whisper sidecar
            WHISPER_URL: 'http://localhost:8000'
          }
        }
      }
    );

    const taskDef = albFargate.taskDefinition;
    const appContainer = taskDef.defaultContainer;

    if (!appContainer) {
      throw new Error('Default container not found');
    }

    // Sidecar: faster-whisper-server from Docker Hub
    // Note: Ensure the image supports ARM64 architecture
    // If the image doesn't support ARM64, you may need to use a different tag or build a custom ARM64 image
    const whisperContainer = taskDef.addContainer('faster-whisper-server', {
      containerName: 'faster-whisper-server',
      image: ecs.ContainerImage.fromRegistry(
        'fedirz/faster-whisper-server:sha-307e23f-cpu'
      ),
      cpu: 1536,           // 1.5 vCPU of the 2
      memoryLimitMiB: 3584,
      portMappings: [
        {
          containerPort: 8000,
          protocol: ecs.Protocol.TCP
        }
      ],
      logging: ecs.LogDrivers.awsLogs({
        streamPrefix: 'faster-whisper'
      }),
      healthCheck: {
        command: [
          'CMD-SHELL',
          'curl -f http://localhost:8000/health || exit 1'
        ],
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        retries: 3,
        startPeriod: cdk.Duration.seconds(60)
      }
    });

    // Ensure app starts after whisper is healthy
    appContainer.addContainerDependencies({
      container: whisperContainer,
      condition: ecs.ContainerDependencyCondition.HEALTHY
    });
  }
}
