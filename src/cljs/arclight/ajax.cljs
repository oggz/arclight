(ns arclight.ajax
  (:require [ajax.core :as ajax]))

(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn default-headers [request]
  ;; (prn "Interceptor: " request)
  (if (local-uri? request)
    (-> request
        (update :headers #(merge {"x-csrf-token"
                                  (.-value
                                   (.getElementById js/document
                                                    "__anti-forgery-token"))} %)))
    request))

;; @ajax/default-interceptors
;; (reset! ajax/default-interceptors [])
(defn load-interceptors! []
  (swap! ajax/default-interceptors
         conj
         (ajax/to-interceptor {:name "default headers"
                               :request default-headers})))

