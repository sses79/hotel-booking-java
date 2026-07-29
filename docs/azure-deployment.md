# Azure Deployment

Phase 6 keeps Azure optional. The Bicep template deploys the native image to
Azure Container Apps Consumption and connects it to Azure SQL Database
serverless.

## Architecture

```text
GitHub main commit
    → Linux AMD64 GraalVM native image
    → GHCR commit-SHA tag
    → Azure Container Apps HTTPS ingress
    → Azure SQL Database
```

Flyway migrates Azure SQL when the container starts. Hibernate validates the
resulting schema. The SQL administrator password is stored as a Container Apps
secret and exposed only through `DB_PASSWORD`.

## Prerequisites

- Azure CLI with Bicep support
- An Azure subscription eligible for the selected services
- A native image published by `.github/workflows/native-image.yml`
- The GHCR package set to public so Container Apps can pull it anonymously

New GHCR packages may initially be private. Open the package settings on
GitHub, connect it to this repository, and change its visibility to public
before deployment.

## Parameters

Set the deployment inputs without writing secrets into the parameter file:

```bash
export API_IMAGE_TAG="<full-git-commit-sha>"
export AZURE_SQL_ADMIN_PASSWORD="<strong-password>"
export AZURE_SQL_ENTRA_ADMIN_LOGIN="<entra-login-name>"
export AZURE_SQL_ENTRA_ADMIN_OBJECT_ID="<entra-object-id>"
```

The development parameter file requests the Azure SQL free allowance. Azure
permits the allowance only when the subscription is eligible; confirm current
pricing and subscription limits before deploying.

## Preview and Deploy

```bash
az group create \
  --name rg-hotel-booking-java-dev-uk-south \
  --location uksouth

az bicep build --file infra/bicep/main.bicep

az deployment group what-if \
  --resource-group rg-hotel-booking-java-dev-uk-south \
  --template-file infra/bicep/main.bicep \
  --parameters infra/bicep/environments/dev.bicepparam

az deployment group create \
  --name hotel-booking-java-dev \
  --resource-group rg-hotel-booking-java-dev-uk-south \
  --template-file infra/bicep/main.bicep \
  --parameters infra/bicep/environments/dev.bicepparam
```

Review `what-if` carefully. Stop if it proposes an unexpected deletion,
database replacement, paid SKU, or wider network access.

## Verify

The verification script checks the deployment, immutable image, replica range,
SQL serverless settings, health, OpenAPI, Swagger UI, and the complete
seed-to-booking flow:

```bash
./scripts/check-azure-resources.sh
```

Override defaults when necessary:

```bash
RESOURCE_GROUP="<resource-group>" \
DEPLOYMENT_NAME="<deployment-name>" \
./scripts/check-azure-resources.sh
```

The database and public SQL firewall rule incur security and cost trade-offs.
For a production system, replace SQL administrator password access and public
networking with managed identity and private networking.
