terraform {
  required_providers {
    google = {
      source = "hashicorp/google"
      version = "6.24.0"
    }
  }
}
provider "google" {}
resource "google_service_account" "default" {
  account_id   = var.account_id
  display_name = "Service Account"
}
resource "google_container_cluster" "primary" {
  name               = var.cluster_name
  location           = "us-central1-a"
  initial_node_count = 3
  node_config {
    machine_type = "e2-medium"
    service_account = google_service_account.default.email
    oauth_scopes    = [
      "https://www.googleapis.com/auth/cloud-platform"
    ]
  }
}

variable "cluster_name" {
  default = "marcellus-wallace"
  type = string
}

variable "account_id" {
  type = string
  sensitive = true
}
