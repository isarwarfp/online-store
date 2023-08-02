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