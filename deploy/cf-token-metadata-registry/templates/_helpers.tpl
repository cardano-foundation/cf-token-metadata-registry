{{/*
Expand the name of the chart.
*/}}
{{- define "cf-token-metadata-registry.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "cf-token-metadata-registry.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "cf-token-metadata-registry.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "cf-token-metadata-registry.labels" -}}
helm.sh/chart: {{ include "cf-token-metadata-registry.chart" . }}
{{ include "cf-token-metadata-registry.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "cf-token-metadata-registry.selectorLabels" -}}
app.kubernetes.io/name: {{ include "cf-token-metadata-registry.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Read-only selector labels: release-scoped identity + component.
Used for pod labels, the PDB and the anti-affinity selector, so coexisting
releases in one namespace never match each other's pods.
*/}}
{{- define "cf-token-metadata-registry.roSelectorLabels" -}}
{{ include "cf-token-metadata-registry.selectorLabels" . }}
app.kubernetes.io/component: read-only
{{- end }}

{{/*
Postgres names default to the release fullname so releases can coexist; dbName/user are fixed.
Override any of them in values (postgres.clusterName / user / dbName / host / secret.name).
*/}}
{{- define "cf-token-metadata-registry.postgres.clusterName" -}}
{{- .Values.postgres.clusterName | default (printf "%s-pg" (include "cf-token-metadata-registry.fullname" .)) }}
{{- end }}

{{- define "cf-token-metadata-registry.postgres.user" -}}
{{- .Values.postgres.user | default "cardano" }}
{{- end }}

{{/* Database name is not release-scoped; fixed default. */}}
{{- define "cf-token-metadata-registry.postgres.dbName" -}}
{{- .Values.postgres.dbName | default "cf_token_metadata_registry" }}
{{- end }}

{{- define "cf-token-metadata-registry.postgres.host" -}}
{{- .Values.postgres.host | default (include "cf-token-metadata-registry.postgres.clusterName" .) }}
{{- end }}

{{/*
Zalando operator connection secret: <namespace>.<user>.<cluster>.credentials.postgresql.acid.zalan.do
The secret lives in the release namespace.
*/}}
{{- define "cf-token-metadata-registry.postgres.secretName" -}}
{{- .Values.postgres.secret.name | default (printf "%s.%s.credentials.postgresql.acid.zalan.do" (include "cf-token-metadata-registry.postgres.user" .) (include "cf-token-metadata-registry.postgres.clusterName" .)) }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "cf-token-metadata-registry.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "cf-token-metadata-registry.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
