(ns arclight.projects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent]
            [cljs.core.async :refer [chan <! >! timeout]]
            [ajax.core :refer [GET POST]]))

(defonce project-state (reagent/atom {:name "default"
                                      :list []
                                      :contents ""}))
;; @project-state

(defn comments []
  [:div.alpha
   [:h3 "Comments..."]
   [:p "Not implemented yet."]])

(defn get-project-data! [project]
  (GET "/project-data" {:params {:file project}
                        :handler #(let [list (:list %)
                                        data (:data %)
                                        filetype (:type %)]
                                    (swap! project-state assoc
                                           :name project
                                           :type filetype
                                           :contents data
                                           :list list))}))

(defn render-project []
  [:div {:style {:margin 10}}
   [:div.alpha {:id "project"}
    (if (= (:type @project-state) :cljs)
      (get @project-state :contents)
      [:div {:dangerouslySetInnerHTML {:__html (get @project-state :contents)}}])]])

(defn project-page [app-state]
  (reagent/create-class
   {:reagent-render
    (fn [] [:div
            [:div.alpha
             [:select {:style {:width 250}
                       :defaultValue "default"
                       :on-change #(get-project-data! (.. % -target -value))}
              [:option {:value "default"} "Choose a project..."]
              (for [project (:list @project-state)]
                ^{:key project}
                [:option project])]]
            [render-project]
            [comments]])

    :component-will-mount
    #(get-project-data! (:name @project-state))}))
