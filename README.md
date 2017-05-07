# copa-λ-services

### Deploy

```shell
$ serverless deploy
```

### Redeploy Function

```
$ serverless deploy function -f sms
```

### Invoke

```shell
$ curl -X POST <url> -H 'Content-Type: application/json' -d '{"body": "Hi"}'
```

### Logs

```
serverless logs -f sms -t
```

### Remove all functions from aws account

Remove Cloud Formation project then run:

```
serverless remove
```

## Twilio SMS Config

1. Update POST endpoint on [twilio's sms app config](https://www.twilio.com/console/sms/services/MG7c87df1f861b9b9b5fdbb7404048376e)
