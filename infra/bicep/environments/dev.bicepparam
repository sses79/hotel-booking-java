using '../main.bicep'

param location = 'uksouth'
param environmentName = 'dev'
param projectName = 'hotel-booking'
param apiImageTag = readEnvironmentVariable('API_IMAGE_TAG')
param sqlDatabaseName = 'HotelBookingFree'
param useSqlFreeLimit = true
param sqlFreeLimitExhaustionBehavior = 'BillOverUsage'
param sqlAutoPauseDelay = 15
param sqlAdministratorPassword = readEnvironmentVariable('AZURE_SQL_ADMIN_PASSWORD')
param sqlEntraAdministratorLogin = readEnvironmentVariable('AZURE_SQL_ENTRA_ADMIN_LOGIN')
param sqlEntraAdministratorObjectId = readEnvironmentVariable('AZURE_SQL_ENTRA_ADMIN_OBJECT_ID')
