(ns ^:figwheel-no-load arclight.dev
  (:require
    [arclight.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
