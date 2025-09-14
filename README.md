# online-store

## Command:
```bash
# To call all jobs
http POST localhost:4041/api/jobs

# To update job by ID
http PUT localhost:4041/api/jobs/d2b932f8-3094-4333-9d2d-3e21ecd94734 < src/main/resources/payloads/jobsInfo.json

# To create jobs
http POST localhost:4041/api/jobs/create < src/main/resources/payloads/jobsInfo.json
```

After using Auth now to create user do the following
```bash
http post localhost:4041/api/auth/users email='imran.fp@outlook.com' password='imran'
```

For login
```bash
http post localhost:4041/api/auth/login email='imran.fp@outlook.com' password='imran'
```

Change Password use
```bash
http put localhost:4041/api/auth/users/password oldPassword='imran' newPassword='sarwar' 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE3NTU4MDI2MzAsImlhdCI6MTc1NTcxNjIzMCwianRpIjoiMTU3YTRjNzQ2MTFiZDgyMTZhYzFlNjkzZWY4MjQ2YTAifQ.XpA_Uj3xnET9cOFCCULn1fNsw7MRBvOJzpH8yjo3rj0'
```

For logout use
```bash
http post localhost:4041/api/auth/logout 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE3NTU4MDI4MTMsImlhdCI6MTc1NTcxNjQxMywianRpIjoiMDU0ODg2NTQ4YjUxYThkOTkwMzRjYjM5MWEyNTZiZTMifQ.EBhUCMhqEXqS5YKb_PWcecPKmvAef5alP7YCQXfQB5I'
```

For delete only admin is required
```bash
http delete localhost:4041/api/auth/users/isarwar.fp@gmail.com 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE3NTU4MDMwODksImlhdCI6MTc1NTcxNjY4OSwianRpIjoiMzUxZjYyZjRhZDMzZTE3MGFiNzliZmE2ZjEyMzkzZjMifQ.GAHOM6G8ocWwUfY-fLlXD9BMeaasAQ2aOvu_D5oVtX4'
```