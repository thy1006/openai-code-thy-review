curl -X POST \
        -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsInNpZ25fdHlwZSI6IlNJR04ifQ.eyJhcGlfa2V5IjoiNGQzN2Q2ZWU4OWExNDE0MTg5ZWUxNzM2NDFkNjdiNTgiLCJleHAiOjE3NDkyOTAzOTk1MDEsInRpbWVzdGFtcCI6MTc0OTI4ODU5OTUwNX0.Oa8dSQl1mpkOr23RnyCpDDmnzKDEM0u6xhUaTa6cA2E" \
        -H "Content-Type: application/json" \
        -H "User-Agent: Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)" \
        -d '{
          "model":"glm-4",
          "stream": "true",
          "messages": [
              {
                  "role": "user",
                  "content": "1+1"
              }
          ]
        }' \
  https://open.bigmodel.cn/api/paas/v4/chat/completions