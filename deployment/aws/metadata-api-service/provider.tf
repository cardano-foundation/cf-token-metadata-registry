provider "aws" {
  region  = var.region
  profile = "${var.aws-profile-name}-${var.aws-account-id}"

  default_tags {
    tags = {
      Environment = "${var.environment}"
      Project     = "${var.project}"
    }
  }
}

provider "aws" {
  region  = "us-east-2"
  alias = "us1"
  profile = "${var.aws-profile-name}-${var.aws-account-id}"

  default_tags {
    tags = {
      Environment = "${var.environment}"
      Project     = "${var.project}"
    }
  }
}

provider "aws" {
  region  = "ap-south-1"
  alias = "ap1"
  profile = "${var.aws-profile-name}-${var.aws-account-id}"

  default_tags {
    tags = {
      Environment = "${var.environment}"
      Project     = "${var.project}"
    }
  }
}

provider "aws" {
  region  = "eu-central-1"
  alias = "lt_eu1"
  profile = "${var.aws-profile-name}-${var.aws-account-id}"

  default_tags {
    tags = {
      Environment = "${var.environment}"
      Project     = "${var.project}"
    }
  }
}

provider "aws" {
  region  = "us-east-1"
  alias = "lt_us1"
  profile = "${var.aws-profile-name}-${var.aws-account-id}"

  default_tags {
    tags = {
      Environment = "${var.environment}"
      Project     = "${var.project}"
    }
  }
}

provider "aws" {
  region  = "ap-southeast-1"
  alias = "lt_ap1"
  profile = "${var.aws-profile-name}-${var.aws-account-id}"

  default_tags {
    tags = {
      Environment = "${var.environment}"
      Project     = "${var.project}"
    }
  }
}

provider "aws" {
  region  = "eu-central-1"
  alias = "monitor_eu1"
  profile = "${var.aws-profile-name}-${var.aws-account-id}"

  default_tags {
    tags = {
      Environment = "${var.environment}"
      Project     = "${var.project}"
    }
  }
}

provider "aws" {
  region  = "us-east-1"
  alias = "monitor_us1"
  profile = "${var.aws-profile-name}-${var.aws-account-id}"

  default_tags {
    tags = {
      Environment = "${var.environment}"
      Project     = "${var.project}"
    }
  }
}

provider "aws" {
  region  = "ap-southeast-1"
  alias = "monitor_ap1"
  profile = "${var.aws-profile-name}-${var.aws-account-id}"

  default_tags {
    tags = {
      Environment = "${var.environment}"
      Project     = "${var.project}"
    }
  }
}