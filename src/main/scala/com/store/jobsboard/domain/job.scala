package com.store.jobsboard.domain

import java.util.UUID

object job:
  final case class Job(
    id: UUID,
    date: Long,
    ownerEmail: String,
    jobInfo: JobInfo,
    active: Boolean = false
  )

  final case class JobInfo(
    company: String,
    title: String,
    description: String,
    externalUrl: String,
    remote: Boolean,
    location: String,
    salaryLo: Option[Int],
    salaryHi: Option[Int],
    currency: Option[String],
    country: Option[String],
    tags: Option[String],
    image: Option[String],
    seniority: Option[String],
    other: Option[String]
  )
  object JobInfo:
    val empty: JobInfo = JobInfo("", "", "", "", false, "", None, None, None, None, None, None, None, None)
