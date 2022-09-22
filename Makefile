push: 
	aws ecr get-login-password --region eu-west-1 --profile AdministratorAccess | docker login --username AWS --password-stdin 747042292633.dkr.ecr.eu-west-1.amazonaws.com
	docker build -t dev-cf-metadata-tools-server .
	docker tag dev-cf-metadata-tools-server:latest 747042292633.dkr.ecr.eu-west-1.amazonaws.com/dev-cf-metadata-tools-server:latest
	docker push 747042292633.dkr.ecr.eu-west-1.amazonaws.com/dev-cf-metadata-tools-server:latest
	aws ecs update-service --force-new-deployment --cluster "dev-cf-metadata-tools" --service "dev-cf-metadata-tools-server" --profile AdministratorAccess
	aws ecs execute-command --cluster "dev-cf-metadata-tools" --task 0d2d94949bf046df9c573b082926b031 --container server --interactive --command "bash" --profile AdministratorAccess