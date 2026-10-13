# Deployment to Render

This Play application can be deployed to Render using the provided Dockerfile and configuration.

## Environment Variables

The application expects the following environment variable:

- `MONGO_URI`: The MongoDB connection URI for your database

## Deploying to Render

1. Create a new Web Service on Render
2. Set the following build and start commands:
   - Build Command: `sbt clean stage`
   - Start Command: `./stage/bin/reactivemongo-demo-app -Dhttp.port=$PORT -DMONGO_URI=$MONGO_URI`
3. Set environment variables in Render:
   - Add `MONGO_URI` with your MongoDB connection string
4. Configure the port to be 9000 (or use $PORT if specified)
5. Trigger a new deployment
