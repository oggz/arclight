(ns arclight.bitcoin
  (:require [cljs.pprint :refer [pprint cl-format]]
            [cljs.core.async :refer [chan <! >! timeout]]
            [reagent.core :as reagent :refer [atom]]
            [reagent.format :refer [currency-format format]]
            [cljsjs.highcharts :as hc]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [arclight.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST]]))


(defonce local-state (atom {:btc {:ths 13
                                  :units 10
                                  :wpm 1500
                                  :cost 0.2
                                  :delay 3
                                  :difficulty 1347001430558
                                  :value 14000}
                            :chartc {:chart {:type "line"
                                             :backgroundColor "rgba(0, 0, 0, 0.0)"
                                             :borderWidth 1
                                             :borderColor "rgb(255, 255, 255)"
                                             :color "rgb(255, 255, 255)"}
                                     :title {:text "Bitcoin Profit"
                                             :style {:color "rgb(255, 255, 255)"}}
                                     :xAxis {:max 24
                                             :tickInterval 1
                                             :title {:text "Time (Months)"
                                                     :style {:color "rgb(255, 255, 255)"}}
                                             :labels {:style {:color "rgb(255, 255, 255)"}}}
                                     :yAxis {:title {:text "Profit (Dollars)"
                                                     :style {:color "rgb(255, 255, 255)"}}
                                             :labels {:overflow "justify"
                                                      :style {:color "rgb(255, 255, 255)"}}}
                                     :tooltip {:valueSuffix " Dollars"}
                                     :plotOptions {:line {:dataLabels {:enabled false}}}
                                     :legend {:layout "vertical"
                                              :align "left"
                                              :verticalAlign "top"
                                              :x 80
                                              :y 50
                                              :floating true
                                              :borderWidth 1
                                              :shadow true
                                              :style {:color "rgb(255, 255, 255)"}}
                                     :credits {:enabled false}
                                     :series [{:name "Dollars"
                                               :data [0 1 2 3 4]
                                               :animation false}]}}))

(defn render-chart [this]
  (let [[_ data] (reagent/argv this)]
    (js/Highcharts.Chart. (reagent/dom-node this) (clj->js (get data :chartc)))))

(defn chart [data]
  (reagent/create-class
   {:display-name "highchart"
    :reagent-render (fn chart-div [this] [:div.inline {:style {:min-width "400px"
                                                                    :height "450px"
                                                                    :margin-top "22px"}}])
    :component-did-mount render-chart
    :component-did-update render-chart}))

(defn ins-comma [n]
  (cl-format nil "~:d" (int n)))

(defn btc-per-day [hashrate difficulty]
  ;; ( block_reward * hashrate * seconds/day ) / ( difficulty * 2^32 )
  (/ (* 15.5 hashrate 1000000000000 86400) (* difficulty (Math.pow 2 32))))

(defn dollars-per-day [value btc]
  (* value btc))

(defn calculate-energy-cost [watts days cost]
  (let [hrs (* 24 days)
        kw (* watts 0.001)
        kwhrs (* kw hrs)]
    (* kwhrs cost)))

(defn calculate-energy-requirement [watts]
  (/ watts 240))

(defn btc-revenue [days hashrate difficulty]
  (loop [d 0
         diff difficulty
         btcpd 0]
    (if (= d days)
      btcpd
      (recur (inc d)
             (if (= (mod d 30) 29) (+ diff 500000000000) diff)
             (+ btcpd (btc-per-day hashrate diff))))))

(defn profit [hashrate diff value watts days cost delay]
  (let [diff (+ diff (* 150000000000 delay))
        btcpd (btc-revenue days hashrate diff)
        dpd (dollars-per-day value btcpd)
        profit (* dpd days)
        cost (calculate-energy-cost watts days cost)]
    (- dpd cost)))

(defn init-stats []
  (GET "https://blockchain.info/q/getdifficulty"
       {:params {:cors true}
        :with-credentials false
        :handler (fn [request]
                   (swap! local-state assoc-in [:btc :difficulty] (js/parseFloat request)))})

  (GET "https://blockchain.info/ticker"
       {:params {:cors true}
        :with-credentials false
        :response-format :json
        :keywords? true
        :handler (fn [request]
                   (def foobar request)
                   (swap! local-state assoc-in [:btc :value] (get-in request [:USD :sell])))}))
(init-stats)

(defn calc-page [app-state]
  (let [btc (get @local-state :btc)
        value (:value btc)
        ths (:ths btc)
        units (:units btc)
        tths (* ths units)
        delay (:delay btc)
        wpm (:wpm btc)
        watts (* units wpm)
        energy (calculate-energy-requirement watts)
        cost (:cost btc)
        diff (:difficulty btc)
        btcpd (btc-per-day tths diff)
        dollars (dollars-per-day value btcpd)
        by-month (map #(profit tths diff value watts % cost delay) (map #(* 30 %) (range 24)))
        formatted (vec (map int by-month))]

    (swap! local-state assoc-in [:chartc :series] [{:name "Dollars" :data formatted :animation false}])

    [:div {:style {:textalign :center}}
     [:div.alpha
      [:fieldset
       [:legend "Info"]
       [:p.lim "This calculator is for estimation only. Bitcoins price
              and mining difficulty fluctuate wildly. This is more of a
              project to learn using clojurescript and reagent to develop a
              reative calculator application. I will probably add a calculator
              page for machining operations in the near future as this was
              easy to program, I could actually utilize it, and it would be a
              bit more practical than a bitcoin calculator. Conclusion: it's
              getting pretty hard to make a profit mining Bitcoin nowadays
              without some serious hashes/kw."]]]
     [:div.alpha
      [:div.inline
       [:fieldset 
        [:legend "Bitcoin Mining Caclulator"]
        [:table.centered
         [:tbody
          [:tr
           [:td {:colSpan 2} "Bitcoin Value (USD): "]
           [:td [:input {:type "number" :value value
                         :on-change #(swap! local-state assoc-in [:btc :value] (.. % -target -value))}]]]
          [:tr
           [:td {:colSpan 2} "Hashrate/Miner (Th/s): "]
           [:td [:input {:type "number" :value ths
                         :on-change #(swap! local-state assoc-in [:btc :ths] (.. % -target -value))}]]]
          [:tr
           [:td {:colSpan 2} "Miners: "]
           [:td [:input {:type "number" :value units
                         :on-change #(swap! local-state assoc-in [:btc :units] (.. % -target -value))}]]]
          [:tr
           [:td {:colSpan 2} "Watts/Miner: "]
           [:td [:input {:type "number" :value wpm
                         :on-change #(swap! local-state assoc-in [:btc :wpm] (.. % -target -value))}]]]
          [:tr
           [:td {:colSpan 2} "Energy Cost ($/kwh): "]
           [:td [:input {:type "number" :value cost
                         :on-change #(swap! local-state assoc-in [:btc :cost] (.. % -target -value))}]]]
          [:tr
           [:td {:colSpan 2} "Start Delay (Months): "]
           [:td [:input {:type "number" :value delay
                         :on-change #(swap! local-state assoc-in [:btc :delay] (.. % -target -value))}]]]
          [:tr
           [:td {:colSpan 2} "Difficulty: "]
           [:td [:input {:type "number" :value diff
                         :on-change #(swap! local-state assoc-in [:btc :difficulty] (.. % -target -value))}]]]
          [:tr
           [:td {:colSpan 2} "Amps: "]
           [:td [:input {:type "number" :value (format "%.0f" energy)
                         :on-change #(swap! local-state assoc-in [:btc :energy] (.. % -target -value))}]]]
          [:tr
           [:td {:colSpan 2} "Watts: "]
           (if false
             [:td [:input {:type "number" :value watts
                           :on-change #(swap! local-state assoc-in [:btc :watts] (.. % -target -value))}]]
             [:td [:input {:type "text" :read-only true :value (ins-comma watts)}]])]
          [:tr [:td [:p "Profit corrected for difficulty increase: "]]]
          (for [n ['(1 "Dollars/Day: ")
                   '(7 "Dollars/Week: ")
                   '(30 "Dollars/Month: ")
                   '(365 "Dollars/Year: ")]]
            ^{:key n}
            [:tr
             [:td (second n)]
             [:td [:input {:type "defaultValue"
                           :read-only true :value (currency-format (profit tths diff value watts (first n) cost delay))}]]
             [:td [:input {:type "defaultValue"
                           :read-only true :value (currency-format (calculate-energy-cost watts (first n) cost))}]]])
          [:tr [:td [:p "Profit without correction: "]]]
          (for [n ['(1 "Dollars/Day: ")
                   '(7 "Dollars/Week: ")
                   '(30 "Dollars/Month: ")
                   '(365 "Dollars/Year: ")]]
            ^{:key n}
            [:tr
             [:td (second n)]
             [:td [:input {:type "defaultValue"
                           :read-only true :value (currency-format
                                                   (- (* dollars (first n))
                                                      (calculate-energy-cost watts (first n) cost)))}]]])]]]]

      [chart @local-state]]]))
