(ns arclight.projects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent]
            [cljs.core.async :refer [chan <! >! timeout]]
            [ajax.core :refer [GET POST]]))

(defonce project-state (reagent/atom {:contents ""}))

(defn get-iframe-height [id]
  (-> js/document (.getElementById id) (.-contentDocument) (.-body) (.-clientHeight)))
;(-> js/document (.getElementById "pframe") (.-contentDocument) (.-body) (.-clientHeight))

(defn set-iframe-height! [id val]
  (-> js/document (.getElementById id) (.-style) (.-height)
      (set! (str val "px"))))

(defn set-iframe-to-content-height [name]
  (set-iframe-height! name (get-iframe-height name)))

(defn comments []
  [:div.alpha
   [:h1 "Comments..."]])

(defn render-project [app-state]
  [:div {:style {:margin 10}}
   [:div.alpha {:id "project"}
    (if (re-matches #".*cljs" (get @app-state :project))
      (get @project-state :contents)
      [:div {:dangerouslySetInnerHTML {:__html (get @project-state :contents)}}])]])

(defn get-project-data [project]
  (GET "/project-data" {:params {:file project}
                        :handler #(swap! project-state assoc :contents %)}))
(get-project-data "default.cljs")

(defn project-page [app-state]
  [:div
   [:div.alpha
    [:select {:style {:width 250}
              :defaultValue "default"
              :on-change (fn update-page-contents [this]
                           (swap! app-state assoc :project (.. this -target -value))
                           (get-project-data (.. this -target -value)))}
     [:option {:value "default.cljs"} "Choose a project..."]
     [:option "phase1.html"]
     [:option "math_test.html"]
     [:option "lm386.cljs"]]]
   [render-project app-state]])
