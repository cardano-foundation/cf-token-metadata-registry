FROM python:3.9-slim-bullseye

RUN apt update && apt upgrade
RUN apt install -y git
WORKDIR /cf-gitsync-job/
COPY . /cf-gitsync-job/
RUN pip install -r "requirements.txt"

ENTRYPOINT ["/bin/sh", "/cf-gitsync-job/sync_from_github.sh"]
