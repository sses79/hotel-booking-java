targetScope = 'resourceGroup'

@description('Azure region for Container Apps and SQL.')
param location string = resourceGroup().location

@allowed([
  'dev'
  'test'
  'prod'
])
param environmentName string = 'dev'

param projectName string = 'hotel-booking'

@description('Immutable GHCR image tag, normally the Git commit SHA.')
param apiImageTag string

param apiImageRepository string = 'ghcr.io/sses79/hotel-booking-java'
param sqlAdministratorLogin string = 'hoteladmin'
param sqlEntraAdministratorLogin string
param sqlEntraAdministratorObjectId string

@description('Azure SQL database name. Change this to create a replacement database during a cost or migration cutover.')
param sqlDatabaseName string = 'HotelBooking'

@description('Apply the Azure SQL Database monthly free allowance when the subscription is eligible.')
param useSqlFreeLimit bool = false

@allowed([
  'AutoPause'
  'BillOverUsage'
])
@description('Behavior after the Azure SQL monthly free allowance is exhausted.')
param sqlFreeLimitExhaustionBehavior string = 'BillOverUsage'

@minValue(15)
@maxValue(10080)
@description('Minutes of inactivity before the General Purpose serverless database pauses.')
param sqlAutoPauseDelay int = 15

@secure()
param sqlAdministratorPassword string

var suffix = take(uniqueString(subscription().id, resourceGroup().id), 8)
var containerAppsEnvironmentName = 'cae-${projectName}-${environmentName}-${suffix}'
var apiAppName = 'ca-${projectName}-${environmentName}-${suffix}'
var sqlServerName = 'sql-${projectName}-${environmentName}-${suffix}'
var tags = {
  environment: environmentName
  project: projectName
  managedBy: 'bicep'
  runtime: 'graalvm-native'
}

resource sqlServer 'Microsoft.Sql/servers@2023-08-01' = {
  name: sqlServerName
  location: location
  tags: tags
  properties: {
    administratorLogin: sqlAdministratorLogin
    administratorLoginPassword: sqlAdministratorPassword
    minimalTlsVersion: '1.2'
    publicNetworkAccess: 'Enabled'
  }
}

resource allowAzureServices 'Microsoft.Sql/servers/firewallRules@2023-08-01' = {
  parent: sqlServer
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

resource sqlEntraAdministrator 'Microsoft.Sql/servers/administrators@2023-08-01' = {
  parent: sqlServer
  name: 'ActiveDirectory'
  properties: {
    administratorType: 'ActiveDirectory'
    login: sqlEntraAdministratorLogin
    sid: sqlEntraAdministratorObjectId
    tenantId: subscription().tenantId
  }
}

resource sqlDatabase 'Microsoft.Sql/servers/databases@2023-08-01' = {
  parent: sqlServer
  name: sqlDatabaseName
  location: location
  tags: tags
  sku: {
    name: 'GP_S_Gen5'
    tier: 'GeneralPurpose'
    family: 'Gen5'
    capacity: 2
  }
  properties: {
    autoPauseDelay: sqlAutoPauseDelay
    minCapacity: json('0.5')
    maxSizeBytes: 34359738368
    requestedBackupStorageRedundancy: 'Local'
    useFreeLimit: useSqlFreeLimit
    freeLimitExhaustionBehavior: useSqlFreeLimit ? sqlFreeLimitExhaustionBehavior : null
    zoneRedundant: false
  }
}

resource containerAppsEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: containerAppsEnvironmentName
  location: location
  tags: tags
  properties: {
    zoneRedundant: false
  }
}

resource apiApp 'Microsoft.App/containerApps@2024-03-01' = {
  name: apiAppName
  location: location
  tags: tags
  properties: {
    managedEnvironmentId: containerAppsEnvironment.id
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: true
        allowInsecure: false
        targetPort: 8080
        transport: 'auto'
      }
      secrets: [
        {
          name: 'sql-administrator-password'
          value: sqlAdministratorPassword
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'api'
          image: '${apiImageRepository}:${apiImageTag}'
          env: [
            {
              name: 'SPRING_PROFILES_ACTIVE'
              value: 'azure'
            }
            {
              name: 'DB_HOST'
              value: sqlServer.properties.fullyQualifiedDomainName
            }
            {
              name: 'DB_PORT'
              value: '1433'
            }
            {
              name: 'DB_NAME'
              value: sqlDatabaseName
            }
            {
              name: 'DB_USER'
              value: sqlAdministratorLogin
            }
            {
              name: 'DB_PASSWORD'
              secretRef: 'sql-administrator-password'
            }
          ]
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
          probes: [
            {
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health/liveness'
                port: 8080
                scheme: 'HTTP'
              }
              initialDelaySeconds: 5
              periodSeconds: 30
              timeoutSeconds: 5
              failureThreshold: 3
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/actuator/health/readiness'
                port: 8080
                scheme: 'HTTP'
              }
              initialDelaySeconds: 5
              periodSeconds: 10
              timeoutSeconds: 5
              failureThreshold: 6
            }
          ]
        }
      ]
      scale: {
        minReplicas: 0
        maxReplicas: 2
        rules: [
          {
            name: 'http'
            http: {
              metadata: {
                concurrentRequests: '20'
              }
            }
          }
        ]
      }
    }
  }
  dependsOn: [
    sqlDatabase
    allowAzureServices
  ]
}

output apiUrl string = 'https://${apiApp.properties.configuration.ingress.fqdn}'
output apiAppName string = apiApp.name
output sqlServerName string = sqlServer.name
output sqlDatabaseName string = sqlDatabase.name
