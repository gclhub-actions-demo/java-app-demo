# Deployment Guide

This guide covers deploying the Java application to Azure Container Apps using GitHub Actions with OpenID Connect (OIDC) authentication.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Azure Setup](#azure-setup)
- [GitHub Configuration](#github-configuration)
- [Deployment Workflow](#deployment-workflow)
- [Environment Management](#environment-management)
- [Blue-Green Deployment](#blue-green-deployment)
- [Rollback Procedures](#rollback-procedures)
- [Monitoring and Observability](#monitoring-and-observability)
- [Troubleshooting](#troubleshooting)

## Architecture Overview

### Deployment Pipeline

```
┌─────────────┐
│   GitHub    │
│  Actions    │
└──────┬──────┘
       │ OIDC Auth
       ▼
┌─────────────┐      ┌──────────────┐
│   Azure     │──────▶│   Key Vault  │
│   Identity  │      │  (Secrets)   │
└──────┬──────┘      └──────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│   Azure Container Registry (ACR)    │
│   stores: app:qa, app:staging,      │
│           app:production            │
└──────────────┬──────────────────────┘
               │
       ┌───────┴────────┬──────────────┐
       ▼                ▼              ▼
┌──────────┐    ┌──────────┐    ┌──────────┐
│    QA    │    │ Staging  │    │Production│
│Container │    │Container │    │Container │
│   App    │    │   App    │    │   App    │
└──────────┘    └──────────┘    └──────────┘
```

### Components

1. **GitHub Actions**: CI/CD orchestration
2. **Azure OIDC**: Secure authentication without long-lived secrets
3. **Azure Container Registry**: Docker image storage
4. **Azure Key Vault**: Sensitive configuration management
5. **Azure Container Apps**: Managed container hosting
6. **Azure Log Analytics**: Monitoring and logging

## Prerequisites

### Required Azure Resources

- Azure subscription with appropriate permissions
- Resource Group
- Azure Container Registry
- Azure Container Apps Environment
- Azure Key Vault
- Azure Log Analytics Workspace
- Service Principal or Managed Identity

### Required Tools

```bash
# Azure CLI
az --version  # v2.50.0 or higher

# GitHub CLI
gh --version  # v2.30.0 or higher

# Docker
docker --version  # v24.0.0 or higher
```

## Azure Setup

### 1. Create Resource Group

```bash
# Variables
RESOURCE_GROUP="rg-java-app-demo"
LOCATION="eastus"
APP_NAME="java-app-demo"

# Create resource group
az group create \
  --name $RESOURCE_GROUP \
  --location $LOCATION
```

### 2. Create Container Registry

```bash
ACR_NAME="acrjavaappdemo"  # Must be globally unique, alphanumeric only

az acr create \
  --resource-group $RESOURCE_GROUP \
  --name $ACR_NAME \
  --sku Standard \
  --admin-enabled false
```

### 3. Create Key Vault

```bash
KEY_VAULT_NAME="kv-java-app-demo"  # Must be globally unique

az keyvault create \
  --resource-group $RESOURCE_GROUP \
  --name $KEY_VAULT_NAME \
  --location $LOCATION \
  --enable-rbac-authorization false \
  --enabled-for-deployment true
```

### 4. Create Log Analytics Workspace

```bash
WORKSPACE_NAME="log-java-app-demo"

az monitor log-analytics workspace create \
  --resource-group $RESOURCE_GROUP \
  --workspace-name $WORKSPACE_NAME \
  --location $LOCATION
```

### 5. Create Container Apps Environment

```bash
ENVIRONMENT_NAME="env-java-app-demo"

az containerapp env create \
  --name $ENVIRONMENT_NAME \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --logs-workspace-id $(az monitor log-analytics workspace show \
    --resource-group $RESOURCE_GROUP \
    --workspace-name $WORKSPACE_NAME \
    --query customerId -o tsv) \
  --logs-workspace-key $(az monitor log-analytics workspace get-shared-keys \
    --resource-group $RESOURCE_GROUP \
    --workspace-name $WORKSPACE_NAME \
    --query primarySharedKey -o tsv)
```

### 6. Create Container Apps (One per Environment)

```bash
# QA Environment
az containerapp create \
  --name "${APP_NAME}-qa" \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image mcr.microsoft.com/azuredocs/containerapps-helloworld:latest \
  --target-port 8080 \
  --ingress 'external' \
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 0.5 \
  --memory 1.0Gi

# Staging Environment
az containerapp create \
  --name "${APP_NAME}-staging" \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image mcr.microsoft.com/azuredocs/containerapps-helloworld:latest \
  --target-port 8080 \
  --ingress 'external' \
  --min-replicas 1 \
  --max-replicas 5 \
  --cpu 1.0 \
  --memory 2.0Gi

# Production Environment
az containerapp create \
  --name "${APP_NAME}-prod" \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image mcr.microsoft.com/azuredocs/containerapps-helloworld:latest \
  --target-port 8080 \
  --ingress 'external' \
  --min-replicas 2 \
  --max-replicas 10 \
  --cpu 1.0 \
  --memory 2.0Gi
```

### 7. Configure Application Secrets in Key Vault

```bash
# Example: Database connection string
az keyvault secret set \
  --vault-name $KEY_VAULT_NAME \
  --name "database-connection-string" \
  --value "jdbc:postgresql://..."

# Example: API keys
az keyvault secret set \
  --vault-name $KEY_VAULT_NAME \
  --name "external-api-key" \
  --value "your-api-key"
```

## GitHub Configuration

### 1. Configure OIDC Authentication

OIDC eliminates the need for long-lived Azure credentials in GitHub secrets.

#### Create Azure AD Application

```bash
# Create Azure AD app
APP_ID=$(az ad app create \
  --display-name "GitHub-Actions-${APP_NAME}" \
  --query appId -o tsv)

echo "Application ID: $APP_ID"

# Create service principal
az ad sp create --id $APP_ID

# Get tenant and subscription IDs
TENANT_ID=$(az account show --query tenantId -o tsv)
SUBSCRIPTION_ID=$(az account show --query id -o tsv)

echo "Tenant ID: $TENANT_ID"
echo "Subscription ID: $SUBSCRIPTION_ID"
```

#### Configure Federated Credentials

```bash
# For main branch deployments
az ad app federated-credential create \
  --id $APP_ID \
  --parameters '{
    "name": "github-main",
    "issuer": "https://token.actions.githubusercontent.com",
    "subject": "repo:YOUR_ORG/YOUR_REPO:ref:refs/heads/main",
    "audiences": ["api://AzureADTokenExchange"]
  }'

# For develop branch deployments
az ad app federated-credential create \
  --id $APP_ID \
  --parameters '{
    "name": "github-develop",
    "issuer": "https://token.actions.githubusercontent.com",
    "subject": "repo:YOUR_ORG/YOUR_REPO:ref:refs/heads/develop",
    "audiences": ["api://AzureADTokenExchange"]
  }'

# For environment-based deployments (production)
az ad app federated-credential create \
  --id $APP_ID \
  --parameters '{
    "name": "github-production",
    "issuer": "https://token.actions.githubusercontent.com",
    "subject": "repo:YOUR_ORG/YOUR_REPO:environment:production",
    "audiences": ["api://AzureADTokenExchange"]
  }'
```

#### Assign Azure Permissions

```bash
# Contributor role on resource group
az role assignment create \
  --assignee $APP_ID \
  --role Contributor \
  --scope /subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP

# AcrPush role for pushing images
az role assignment create \
  --assignee $APP_ID \
  --role AcrPush \
  --scope /subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ContainerRegistry/registries/$ACR_NAME

# Key Vault Secrets User role
az role assignment create \
  --assignee $APP_ID \
  --role "Key Vault Secrets User" \
  --scope /subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.KeyVault/vaults/$KEY_VAULT_NAME
```

### 2. Configure GitHub Secrets

```bash
# Set GitHub secrets using GitHub CLI
gh secret set AZURE_CLIENT_ID --body "$APP_ID"
gh secret set AZURE_TENANT_ID --body "$TENANT_ID"
gh secret set AZURE_SUBSCRIPTION_ID --body "$SUBSCRIPTION_ID"

# Set Azure resource names
gh secret set AZURE_RESOURCE_GROUP --body "$RESOURCE_GROUP"
gh secret set AZURE_CONTAINER_REGISTRY --body "$ACR_NAME"
gh secret set AZURE_KEY_VAULT_NAME --body "$KEY_VAULT_NAME"
```

### 3. Configure GitHub Environments

See [ENVIRONMENTS.md](ENVIRONMENTS.md) for detailed environment configuration.

```bash
# Create environments
gh api repos/:owner/:repo/environments/qa -X PUT
gh api repos/:owner/:repo/environments/staging -X PUT
gh api repos/:owner/:repo/environments/production -X PUT
```

## Deployment Workflow

### Workflow Structure

The deployment workflow (`.github/workflows/deploy-azure.yml`) handles:

1. **Build**: Compile application and run tests
2. **Docker**: Build and push container images
3. **Deploy**: Deploy to target environment
4. **Verify**: Run smoke tests

### Triggering Deployments

#### Automatic Deployments

```yaml
# QA: Auto-deploy from develop branch
on:
  push:
    branches: [develop]

# Staging: Auto-deploy from main branch
on:
  push:
    branches: [main]
```

#### Manual Deployments

```bash
# Trigger production deployment manually
gh workflow run deploy-azure.yml \
  -f environment=production \
  -f version=v1.2.3
```

### Deployment Process

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'
      
      - name: Build with Maven
        run: mvn clean package -DskipTests
      
      - name: Run tests
        run: mvn test

  docker:
    needs: build
    runs-on: ubuntu-latest
    permissions:
      id-token: write  # Required for OIDC
      contents: read
    steps:
      - uses: actions/checkout@v4
      
      - name: Azure login with OIDC
        uses: azure/login@v1
        with:
          client-id: ${{ secrets.AZURE_CLIENT_ID }}
          tenant-id: ${{ secrets.AZURE_TENANT_ID }}
          subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
      
      - name: Login to ACR
        run: |
          az acr login --name ${{ secrets.AZURE_CONTAINER_REGISTRY }}
      
      - name: Build and push
        run: |
          docker build -t ${{ secrets.AZURE_CONTAINER_REGISTRY }}.azurecr.io/app:${{ github.sha }} .
          docker push ${{ secrets.AZURE_CONTAINER_REGISTRY }}.azurecr.io/app:${{ github.sha }}

  deploy:
    needs: docker
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Azure login
        uses: azure/login@v1
        with:
          client-id: ${{ secrets.AZURE_CLIENT_ID }}
          tenant-id: ${{ secrets.AZURE_TENANT_ID }}
          subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
      
      - name: Get secrets from Key Vault
        id: secrets
        run: |
          DB_CONN=$(az keyvault secret show \
            --vault-name ${{ secrets.AZURE_KEY_VAULT_NAME }} \
            --name database-connection-string \
            --query value -o tsv)
          echo "::add-mask::$DB_CONN"
          echo "db-connection=$DB_CONN" >> $GITHUB_OUTPUT
      
      - name: Deploy to Container App
        run: |
          az containerapp update \
            --name java-app-demo-prod \
            --resource-group ${{ secrets.AZURE_RESOURCE_GROUP }} \
            --image ${{ secrets.AZURE_CONTAINER_REGISTRY }}.azurecr.io/app:${{ github.sha }} \
            --set-env-vars \
              "DATABASE_URL=secretref:database-connection-string" \
              "ENVIRONMENT=production"
```

## Environment Management

### Environment Variables

Container Apps support three types of configuration:

#### 1. Plain Environment Variables

```bash
az containerapp update \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --set-env-vars \
    "ENVIRONMENT=production" \
    "LOG_LEVEL=INFO" \
    "PORT=8080"
```

#### 2. Secret References (from Key Vault)

```bash
# Link secrets from Key Vault
az containerapp secret set \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --secrets \
    database-connection-string=keyvaultref:$KEY_VAULT_URI/secrets/database-connection-string,identityref:system

# Reference in environment variables
az containerapp update \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --set-env-vars \
    "DATABASE_URL=secretref:database-connection-string"
```

#### 3. Container App Secrets

```bash
# For secrets not in Key Vault
az containerapp secret set \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --secrets \
    api-key="your-secret-value"
```

### Resource Scaling

```bash
# Configure autoscaling
az containerapp update \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --min-replicas 2 \
  --max-replicas 10 \
  --scale-rule-name http-scale \
  --scale-rule-type http \
  --scale-rule-http-concurrency 100
```

### Health Probes

```bash
az containerapp update \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --health-probe-type liveness \
  --health-probe-path "/actuator/health/liveness" \
  --health-probe-interval 30 \
  --health-probe-timeout 5
```

## Blue-Green Deployment

Azure Container Apps supports traffic splitting for blue-green deployments.

### Implementation

```bash
# Deploy new version with 0% traffic (green)
az containerapp revision copy \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --image $ACR_NAME.azurecr.io/app:v2.0.0

# Get revision names
BLUE_REVISION=$(az containerapp revision list \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --query "[?properties.trafficWeight > \`0\`].name" -o tsv)

GREEN_REVISION=$(az containerapp revision list \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --query "[?properties.trafficWeight == \`0\`] | [0].name" -o tsv)

# Gradually shift traffic
az containerapp ingress traffic set \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --revision-weight $BLUE_REVISION=90 $GREEN_REVISION=10

# Monitor metrics, then complete cutover
az containerapp ingress traffic set \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --revision-weight $GREEN_REVISION=100

# Deactivate old revision
az containerapp revision deactivate \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --revision $BLUE_REVISION
```

### Automated Blue-Green in GitHub Actions

```yaml
- name: Deploy with blue-green
  run: |
    # Deploy new revision with label
    NEW_REVISION=$(az containerapp revision copy \
      --name ${{ env.APP_NAME }} \
      --resource-group ${{ secrets.AZURE_RESOURCE_GROUP }} \
      --image ${{ env.IMAGE_NAME }} \
      --revision-suffix ${{ github.sha }} \
      --query name -o tsv)
    
    # Run smoke tests against new revision
    REVISION_URL=$(az containerapp revision show \
      --name ${{ env.APP_NAME }} \
      --resource-group ${{ secrets.AZURE_RESOURCE_GROUP }} \
      --revision $NEW_REVISION \
      --query properties.fqdn -o tsv)
    
    curl -f https://$REVISION_URL/actuator/health || exit 1
    
    # Shift 10% traffic for canary testing
    az containerapp ingress traffic set \
      --name ${{ env.APP_NAME }} \
      --resource-group ${{ secrets.AZURE_RESOURCE_GROUP }} \
      --revision-weight $NEW_REVISION=10
    
    # Wait 5 minutes and monitor error rates
    sleep 300
    
    # Complete rollout
    az containerapp ingress traffic set \
      --name ${{ env.APP_NAME }} \
      --resource-group ${{ secrets.AZURE_RESOURCE_GROUP }} \
      --revision-weight $NEW_REVISION=100
```

## Rollback Procedures

### Quick Rollback

```bash
# List revisions
az containerapp revision list \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --output table

# Rollback to previous revision
PREVIOUS_REVISION="java-app-demo-prod--abc123"

az containerapp ingress traffic set \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --revision-weight $PREVIOUS_REVISION=100
```

### Automated Rollback

```yaml
- name: Deploy with automatic rollback
  run: |
    CURRENT_REVISION=$(az containerapp show \
      --name ${{ env.APP_NAME }} \
      --resource-group ${{ secrets.AZURE_RESOURCE_GROUP }} \
      --query "properties.latestRevisionName" -o tsv)
    
    # Deploy new version
    az containerapp update \
      --name ${{ env.APP_NAME }} \
      --resource-group ${{ secrets.AZURE_RESOURCE_GROUP }} \
      --image ${{ env.IMAGE_NAME }}
    
    # Wait for deployment
    sleep 60
    
    # Run health checks
    if ! curl -f https://${{ env.APP_URL }}/actuator/health; then
      echo "Health check failed, rolling back"
      az containerapp ingress traffic set \
        --name ${{ env.APP_NAME }} \
        --resource-group ${{ secrets.AZURE_RESOURCE_GROUP }} \
        --revision-weight $CURRENT_REVISION=100
      exit 1
    fi
```

## Monitoring and Observability

### Application Insights

```bash
# Create Application Insights
APP_INSIGHTS_NAME="appi-java-app-demo"

az monitor app-insights component create \
  --app $APP_INSIGHTS_NAME \
  --location $LOCATION \
  --resource-group $RESOURCE_GROUP \
  --workspace $(az monitor log-analytics workspace show \
    --resource-group $RESOURCE_GROUP \
    --workspace-name $WORKSPACE_NAME \
    --query id -o tsv)

# Get instrumentation key
INSTRUMENTATION_KEY=$(az monitor app-insights component show \
  --app $APP_INSIGHTS_NAME \
  --resource-group $RESOURCE_GROUP \
  --query instrumentationKey -o tsv)

# Configure in Container App
az containerapp update \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --set-env-vars \
    "APPLICATIONINSIGHTS_CONNECTION_STRING=InstrumentationKey=$INSTRUMENTATION_KEY"
```

### Log Streaming

```bash
# Stream logs
az containerapp logs show \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --follow

# Query logs
az monitor log-analytics query \
  --workspace $(az monitor log-analytics workspace show \
    --resource-group $RESOURCE_GROUP \
    --workspace-name $WORKSPACE_NAME \
    --query customerId -o tsv) \
  --analytics-query "ContainerAppConsoleLogs_CL | where ContainerAppName_s == 'java-app-demo-prod' | top 100 by TimeGenerated desc"
```

### Metrics

```bash
# View metrics
az monitor metrics list \
  --resource $(az containerapp show \
    --name java-app-demo-prod \
    --resource-group $RESOURCE_GROUP \
    --query id -o tsv) \
  --metric Requests
```

## Troubleshooting

### Common Issues

#### 1. OIDC Authentication Failure

**Symptoms**: `Error: OIDC token validation failed`

**Solution**:
```bash
# Verify federated credentials
az ad app federated-credential list --id $APP_ID

# Check subject matches exactly
# Format: repo:OWNER/REPO:ref:refs/heads/BRANCH
```

#### 2. Container App Fails to Start

**Symptoms**: Revision shows "Failed" status

**Solutions**:
```bash
# Check revision details
az containerapp revision show \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --revision <revision-name>

# View container logs
az containerapp logs show \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --revision <revision-name>

# Common fixes:
# - Verify image exists in ACR
# - Check health probe configuration
# - Verify environment variables
# - Check resource limits (CPU/memory)
```

#### 3. Image Pull Errors

**Symptoms**: `Failed to pull image: unauthorized`

**Solution**:
```bash
# Verify ACR permissions
az role assignment list \
  --assignee $APP_ID \
  --scope /subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ContainerRegistry/registries/$ACR_NAME

# Grant AcrPull role if missing
az role assignment create \
  --assignee $APP_ID \
  --role AcrPull \
  --scope /subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ContainerRegistry/registries/$ACR_NAME
```

#### 4. Key Vault Access Denied

**Symptoms**: `Error fetching secret: Access denied`

**Solution**:
```bash
# Grant Key Vault permissions
az role assignment create \
  --assignee $APP_ID \
  --role "Key Vault Secrets User" \
  --scope /subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.KeyVault/vaults/$KEY_VAULT_NAME

# Or use access policies (legacy)
az keyvault set-policy \
  --name $KEY_VAULT_NAME \
  --object-id $(az ad sp show --id $APP_ID --query id -o tsv) \
  --secret-permissions get list
```

### Debugging Commands

```bash
# Check Container App status
az containerapp show \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --query "properties.{status:provisioningState,fqdn:configuration.ingress.fqdn,image:template.containers[0].image}"

# List all revisions
az containerapp revision list \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --output table

# Test connectivity
APP_URL=$(az containerapp show \
  --name java-app-demo-prod \
  --resource-group $RESOURCE_GROUP \
  --query properties.configuration.ingress.fqdn -o tsv)

curl -v https://$APP_URL/actuator/health
```

## Additional Resources

- [Azure Container Apps Documentation](https://learn.microsoft.com/en-us/azure/container-apps/)
- [GitHub Actions Azure Login](https://github.com/Azure/login)
- [Azure OIDC Configuration](https://learn.microsoft.com/en-us/azure/developer/github/connect-from-azure)
- [Container Apps Best Practices](https://learn.microsoft.com/en-us/azure/container-apps/best-practices)
