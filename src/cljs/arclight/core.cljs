(ns arclight.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.pprint :refer [pprint cl-format]]
            [cljs.core.async :refer [chan <! >! timeout]]
            [reagent.core :as reagent :refer [atom]]
            [reagent.format :refer [currency-format format]]
            [cljsjs.highcharts :as hc]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [arclight.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST]]
            [arclight.bitcoin :as btc]
            [arclight.nav :as nav]
            [arclight.login :as login]
            [arclight.register :as reg]
            [arclight.projects :as proj]
            [arclight.about :as about]
            [arclight.home :as home]))


(def log (.-log js/console))
(def err (.-error js/console))

(defonce app-state (atom {:user "foobar"
                          :pass "groove"
                          :logged false
                          :error ""
                          :counter 0
                          :n 0
                          :project "default.cljs"
                          :project-height 200}))



;; Is this OK to do?
;; What about this?
;; (defonce go-increment (go-loop []
;;                         (<! (timeout 66))
;;                         (swap! app-state update :n inc)
;;                         (recur)))

;; (GET "/phase1.html" {:handler #(swap! app-state assoc :project (str %))})
;; (GET "/phase1.html" {:handler #(log (str %))})


;; -------------------------
;; Routes

(def page (atom #'home/home-page))

(defn current-page [app-state]
  [:div [@page app-state]])

(secretary/defroute "/" []
  (reset! page #'home/home-page))
(secretary/defroute "/about" []
  (reset! page #'about/about-page))
(secretary/defroute "/projects" []
  (reset! page #'proj/project-page))
(secretary/defroute "/calc" []
  (reset! page #'btc/calc-page))
(secretary/defroute "/login" []
  (reset! page #'login/login-page))
(secretary/defroute "/register" []
  (reset! page #'reg/register-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [nav/nav-bar app-state] (.getElementById js/document "navbar"))
  (reagent/render [current-page app-state] (.getElementById js/document "app"))
  (reagent/render [nav/footer app-state] (.getElementById js/document "footer"))
  (accountant/dispatch-current!))

(defn init! []
  (load-interceptors!)
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
