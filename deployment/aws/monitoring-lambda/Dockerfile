FROM public.ecr.aws/lambda/python:3.9

RUN yum group install -y "Development Tools"
RUN yum install -y openssl-devel
RUN yum install -y libcurl-devel

# Copy function code
COPY . ${LAMBDA_TASK_ROOT}

RUN  pip3 install -r requirements.txt --target "${LAMBDA_TASK_ROOT}"

# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "run_monitor.handler" ]
