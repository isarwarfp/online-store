package com.store.jobsboard.domain

object pagination:
  final case class Pagination(limit: Int, offset: Int)
  object Pagination:
    private val defaultPageSize: Int = 20
    def apply(mayBeLimit: Option[Int], mayBeOffset: Option[Int]) =
      new Pagination(mayBeLimit.getOrElse(defaultPageSize), mayBeOffset.getOrElse(0))
    def default =
      new Pagination(defaultPageSize, 0)
