FROM artilleryio/artillery:latest

RUN apk add --no-cache python3 py3-pip \
    && pip3 install --upgrade pip \
    && pip3 install awscli \
    && rm -rf /var/cache/apk/*

WORKDIR /loadtest/
COPY . /loadtest/

ENTRYPOINT ["/bin/sh", "/loadtest/run_loadtest.sh"]
