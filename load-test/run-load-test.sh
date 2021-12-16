STACK_NAME=ServerlessKotlinDemo

API_URL=$(aws cloudformation describe-stacks --stack-name $STACK_NAME \
  --query 'Stacks[0].Outputs[?OutputKey==`KotlinApiUrl`].OutputValue' \
  --output text)

artillery run load-test.yml --target "$API_URL"