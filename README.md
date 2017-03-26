# copa-services

### Deploy

```shell
$ serverless deploy
```

### Redeploy Function

```
$ serverless deploy function -f echo
```

### Invoke

```shell
$ curl -X POST <url> -H 'Content-Type: application/json' -d '{"body": "Hi"}'
```

### logs

```
serverless logs -f echo -t
```

### remove all functions from aws account

```
serverless remove
```
