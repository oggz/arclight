(ns arclight.ajax
  (:require [ajax.core :as ajax]))

(def log (.-log js/console))
(def err (.-error js/console))

(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn default-headers [request]
  ;; (prn "Interceptor: " request)
  (if (local-uri? request)
    (-> request
        (assoc :format (ajax/json-request-format)
               :keywords? true
               :response-format (ajax/json-response-format {:keywords? true})
               :error-handler #(prn "ERROR: AJAX Handler: " (str %)))
        (update :headers #(merge {"x-csrf-token"
                                  (.-value
                                   (.getElementById js/document
                                                    "__anti-forgery-token"))} %)))
    request))


;; @ajax/default-interceptors
;; (reset! ajax/default-interceptors [])
;; (load-interceptors!)
(defn load-interceptors! []
  (swap! ajax/default-interceptors
         conj
         (ajax/to-interceptor {:name "default headers"
                               :request default-headers})))

