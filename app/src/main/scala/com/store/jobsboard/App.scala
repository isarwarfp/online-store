package com.store.jobsboard

import scala.scalajs.js.annotation.*
import org.scalajs.dom.document
import scala.scalajs.js

@JSExportTopLevel("OnlineStoreApp")
class App {
    @JSExport
    def doSomething(containerId: String) = {
        val container = document.getElementById(containerId)
        container.innerHTML = "Scala Here, with JS!"
    }
}
