(ns arclight.home
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.core.async :refer [chan <! >! timeout close! alts!]]
            [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET POST]]))

(def spinner #{[-1 0] [0 0] [1 0]})
(def r-pentamino #{[-1 0] [-1 1] [0 -1] [0 0] [1 0]})

(defonce state (reagent/atom {:width 500
                              :height 500
                              :size 10
                              :speed 150
                              :generation 0
                              :cells r-pentamino
                              :ctl (chan 0)}))


(defn neighbors [[x y]]
  (let [x ((juxt inc inc identity dec dec dec identity inc) x)
        y ((juxt identity inc inc inc identity dec dec dec) y)]
    (map vector x y)))

(defn evolve [pop]
  (set (for [[cell count] (frequencies (mapcat neighbors pop))
             :when (or (= 3 count)
                       (and (= 2 count)
                            (pop cell)))]
         cell)))

(defonce go-evolve
  (let [ctl (chan 0)]
    (go-loop []
      (alt! ctl ([v] (if (= v :kill)
                       (prn "Stopped!")
                       (do (<! ctl) (recur))))
            (timeout (:speed @state)) (do
                                        (swap! state update :cells evolve)
                                        (swap! state update :generation inc)
                            ;; (update-grid!)
                            (recur))))
    ctl))
;; (swap! state assoc :size 5)
;; (count (:cells @state))

(defn reset-life! [state]
  (swap! state assoc :cells r-pentamino)
  (swap! state assoc :generation 0))

(defn home-page [app-state]
  (let [s @state]
    [:div
     [:div.alpha
      [:h2 "Welcome to Arclight Labs"]
      [:div
       [:button {:on-click #(go (>! go-evolve :pause))} "Pause/Play"]
       [:button {:on-click #(go (>! go-evolve :kill))} "Stop"]
       [:button {:on-click #(reset-life! state)} "Reset"]
       [:button {:on-click #(swap! state update :size inc)} "+"]
       [:button {:on-click #(swap! state update :size dec)} "-"]]
      [:svg.life {:width (:width s)
                  :height (:height s)
                  :viewBox "-250 -250 500 500"
                  :on-click #(prn (.-clientX %))}
       (for [[x y] (:cells s)]
         ^{:key (str [x y])}
         [:rect  {:width (- (:size s) 1)
                  :height (- (:size s) 1)
                  :fill "green"
                  :x (* (:size s) x)
                  :y (* (:size s) y)}])]
      [:div
       [:p (str "Generation: " (:generation s) " | Cells: " (count (:cells s)))]]]
     [:div.alpha
      [:p "This is an implementation of John Conway's Game of Life in Clojurescript. It begins from the r-pentamino lifeform which evolves into many other lifeforms such as spinners and gliders. Please check out the code on github if you are interested. The link for that is below and this code is in the home.cljs file."]]]))
