(ns arclight.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [arclight.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]
            [ring.util.response :refer [response]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(def nav-bar
  [:div#navbar])

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container" :background "../images/impeller.jpg"}
     nav-bar
     mount-target
     (anti-forgery-field)
     (include-js "/js/app.js")]))

(def state (atom {:req "foo"
                  :body "bar"}))
(defn handle-login
  [request]
  (swap! state assoc-in [:req] (:params request))
  ;; (swap! state assoc-in [:body] (slurp (:body request)))  WHY NO WORKIE!
  request)

;; (:req @state)

(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/about" [] (loading-page))
  (GET "/test" [] (loading-page))
  (GET "/t2" req (str "GET: " req))
  ;; (POST "/login" req (str "POST: " (merge (slurp (:body req)) {:params {:foo "bar"}})))
  (POST "/login" req (handle-login req))
  
  (resources "/")
  (not-found "404: The pa1ge you seek was not found!"))

(def app (wrap-middleware #'routes))
