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
            [ajax.core :refer [GET POST]]))


(def log (.-log js/console))
(def err (.-error js/console))


(def lorem-ipsum
  "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?")



(defonce app-state (atom {:user "foobar"
                          :pass "groove"
                          :logged false
                          :error ""
                          :counter 0
                          :n 0
                          :btc {:ths 130
                                :watts 15000
                                :power-cost 0.2
                                :delay 0
                                :difficulty 1347001430558
                                :value 11000
                                :btc 0
                                :dollars 0
                                :profit 0}
                          :chart {:chart {:type "line"
                                          :backgroundColor "rgba(0, 0, 0, 0.0)"
                                          :borderWidth 1
                                          :borderColor "rgb(255, 255, 255)"
                                          :color "rgb(255, 255, 255)"}
                                  :title {:text "Bitcoin Profit"
                                          :style {:color "rgb(255, 255, 255)"}}
                                  :xAxis {:min 0
                                          :max 24
                                          :tickInterval 1
                                          :title {:text "Time"
                                                  :style {:color "rgb(255, 255, 255)"}}
                                          :labels {:style {:color "rgb(255, 255, 255)"}}}
                                  :yAxis {:min 0
                                          :title {:text "Profit (Dollars)"
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
                                            :data [107 31 635 203 2]}]}}))

(defonce go-increment (go-loop []
                        (<! (timeout 66))
                        (swap! app-state update :n inc)
                        (recur)))

;; (GET "/phase1.html" {:handler #(swap! app-state assoc :project (str %))})
;; (GET "/phase1.html" {:handler #(log (str %))})

;; -------------------------
;; Views

(defn home-page []
  [:div.alpha
   [:h2 "Welcome to Arclight... " (:counter @app-state)]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]
   [:div
    [:p (repeat 5 lorem-ipsum)]]])

(defn handle-login [response]
  (swap! app-state assoc :logged true)
  (prn response))

(defn handle-logout [response]
  (swap! app-state assoc :logged false)
  (prn response))

(defn login-form []
  (let [user (:user @app-state)
        pass (:pass @app-state)]
    [:fieldset 
     [:legend "Login"]
     [:table.centered
      [:tbody
       [:tr
        [:td "Username: "]
        [:td [:input {:type "text" :value user
                      :on-change #(swap! app-state assoc :user (.. % -target -value))}]]]
       [:tr
        [:td "Password: "]
        [:td [:input {:type "password" :value pass
                      :on-change #(swap! app-state assoc :pass (.. % -target -value))}]]]]]
     [:button.wide {:on-click #(POST "/login" {:params {:user user :pass pass}
                                               :handler handle-login})} "Log in!"]
     [:br] [:button.wide {:on-click #(GET "/logout" {:params {}
                                                  :handler handle-logout})} "Log out!"]
     [:br] [:button.wide {:on-click
                       #(GET "/t2" {:params {:foo 3}
                                    :handler
                                    (fn [{x :rand}]
                                      (swap! app-state assoc :counter x))})} "GET secured!"]]))

(defn handle-register [response]
  (prn response))

(defn register-form []
  (let [user (:user @app-state)
        pass (:pass @app-state)
        email (:email @app-state)]
    [:fieldset
     [:legend "Register Here"]
     [:table.centered
      [:tbody
       [:tr
        [:td "Username: "]
        [:td [:input {:type "text" :value user
                      :on-change #(swap! app-state assoc :user (.. % -target -value))}]]]
       [:tr
        [:td {:style {:text-align "right"}} "Email: "]
        [:td [:input {:type "text" :value email
                      :on-change #(swap! app-state assoc :email (.. % -target -value))}]]]
       [:tr
        [:td "Password: "]
        [:td [:input {:type "password" :value pass
                      :on-change #(swap! app-state assoc :pass (.. % -target -value))}]]]]]
     [:button.wide
      {:on-click
       #(POST "/register" {:params {:user user :pass pass :email email}
                           :handler handle-register})} "Register"]]))

(defn about-page []
  [:div.alpha
   [:h2 "About Arclight... " (:counter @app-state)]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]])

(defn set-iframe-to-content-height [name]
  (let [height (-> js/document
                   (.getElementById name)
                   (.-contentDocument)
                   (.-body)
                   (.-clientHeight))]
    (-> js/document
        (.getElementById name)
        (.-style)
        (.-height)
        (set! (str height "px")))))

(defn comments []
  [:div.alpha
   [:h1 "Comments..."]])

(defn test-page []
  (reagent/create-class
   {:display-name "test"
    :reagent-render
    (fn test-render [this]
      [:div
       [:div.alpha {:id "project"} ;{:dangerouslySetInnerHTML {:__html (:project @app-state)}}
        [:iframe {:id "pframe" :src "/phase1.html"
                  :style {:width "1px" :min-width "100%" :height "20000px"}
                  :scrolling "no" :frameBorder "no"}] [:br]]
       [comments]])
    :component-did-mount
    (fn test-did-mount [this]
      (go
        (<! (timeout 1000))
        (set-iframe-to-content-height "pframe"))
      (go
        (<! (timeout 5000))
        (set-iframe-to-content-height "pframe")))}))




(defn chart []
  (reagent/create-class
   {:display-name "highchart"
    :reagent-render (fn chart-div [this data] [:div.inline {:style {:min-width "400px"

                                                                    :height "450px"
                                                                    :margin-top "22px"}}])
    :component-did-mount (fn render-chart [this] (js/Highcharts.Chart. (reagent/dom-node this) (clj->js (get @app-state :chart))))
    :component-did-update (fn update-chart [this] (js/Highcharts.Chart. (reagent/dom-node this) (clj->js (get @app-state :chart))))}))

(defn ins-comma [n]
  (cl-format nil "~:d" (int n)))

(defn btc-per-day [hashrate difficulty]
  (/ (* 12.5 hashrate 1000000000000 86400) (* difficulty (Math.pow 2 32))))

(defn dollars-per-day [value btc]
  (* value btc))

(defn calculate-energy-cost [watts days cost]
  (let [hrs (* 24 days)
        kw (* watts 0.001)
        kwhrs (* kw hrs)]
    (* kwhrs cost)))

(defn profit [hashrate diff value watts days cost delay]
  (let [btcpd (btc-per-day hashrate (+ diff (* diff 0.002 days) (* 100000000000 delay)))
        dpd (dollars-per-day value btcpd)
        profit (* dpd days)
        cost (calculate-energy-cost watts days cost)]
    (- profit cost)))

(defn calc-page [app-state]
  (let [btc (get @app-state :btc)
        value (:value btc)
        ths (:ths btc)
        delay (:delay btc)
        watts (:watts btc)
        cost 0.2
        diff (:difficulty btc)
        btcpd (btc-per-day ths diff)
        dollars (do (swap! app-state assoc-in [:btc :dollars] (dollars-per-day value btcpd))
                    (get-in @app-state [:btc :dollars]))
        by-month (map #(profit ths diff value watts % cost delay) (map #(* 30 %) (range 24)))
        formatted (vec (map int by-month))
        data (do (swap! app-state assoc-in [:chart :series] [{:name "Dollars" :data formatted}])
                 (:data (first (get-in @app-state [:chart :series]))))]
    [:div {:style {:textalign :center}}
     [:div.alpha
      [:div.inline 
       [:fieldset 
        [:legend "Bitcoin Mining Caclulator"]
        [:table.centered
         [:tbody
          [:tr
           [:td "Bitcoin Value (USD): "]
           [:td [:input {:type "text" :value value
                         :on-change #(swap! app-state assoc-in [:btc :value] (.. % -target -value))}]]]
          [:tr
           [:td "Hashrate (Th/s): "]
           [:td [:input {:type "text" :value ths
                         :on-change #(swap! app-state assoc-in [:btc :ths] (.. % -target -value))}]]]
          [:tr
           [:td "Start Delay (Months): "]
           [:td [:input {:type "text" :value delay
                         :on-change #(swap! app-state assoc-in [:btc :delay] (.. % -target -value))}]]][:tr
           [:td "Difficulty: "]
           [:td [:input {:type "text" :value diff
                         :on-change #(swap! app-state assoc-in [:btc :difficulty] (.. % -target -value))}]]]
          [:tr
           [:td "Watts: "]
           (if false
             [:td [:input {:type "text" :value watts
                           :on-change #(swap! app-state assoc-in [:btc :watts] (.. % -target -value))}]]
             [:td [:input {:type "text" :read-only true :value (ins-comma(* (/ ths 13.5) 1500))}]])]
          [:h8 "Profit corrected for difficulty increase: "]
          (for [n ['(1 "Dollars/Day: ")
                   '(7 "Dollars/Week: ")
                   '(30 "Dollars/Month: ")
                   '(365 "Dollars/Year: ")]]
            ^{:key n}
            [:tr
             [:td (second n)]
             [:td [:input {:type "defaultValue"
                           :read-only true :value (currency-format (profit ths diff value watts (first n) cost delay))}]]])
          [:h8 "Profit without correction: "]
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

      [chart data]]]))




(defn register-page []
  [:div
   [:div.alpha.inline
    [:fieldset
     [:legend "Register"]
     [:p (take 2000 lorem-ipsum)]]]
   [:div.alpha.inline
    [register-form]]])

(defn login-page []
  [:div
   [:div.alpha {:style {:float "left" :max-width 640}}
    [:fieldset
     [:legend "Register"]
     [:p (take 2000 lorem-ipsum)]]]
   [:div.alpha {:style {:float "left"}}
    [login-form]]])

(defn nav-bar []
  [:nav.header
   (let [w (+ 50 (.-innerWidth js/window))]
     [:img {:src "/images/mario_run.gif"
            :style {:height "3em" :bottom -10
                    :left (- (mod (* 6 (:n @app-state)) (- w 10)) 25)}}])
   [:a.nav {:href "/"} "Home"]
   [:a.nav {:href "/about"} "About"]
   [:a.nav {:href "/test"} "Test"]
   [:a.nav {:href "/calc"} "Calc"]
   [:a.nav {:href "/register"} "Register"]
   [:a.nav {:href "/login"} "Login"]
   (if (:logged @app-state)
     [:a.nav {:href "/"} (str "Dashboard: " (:user @app-state))])
   ])

(defn footer []
  [:div.footer
   [:a.nav {:href "https://github.com/oggz/arclight"} "Github"]])

;; -------------------------
;; Routes

(def page (atom #'home-page))
(defn current-page [app-state]
  [:div [@page app-state]])

(secretary/defroute "/" []
  (reset! page #'home-page))
(secretary/defroute "/about" []
  (reset! page #'about-page))
(secretary/defroute "/test" []
  (reset! page #'test-page))
(secretary/defroute "/calc" []
  (reset! page #'calc-page))
(secretary/defroute "/login" []
  (reset! page #'login-page))
(secretary/defroute "/register" []
  (reset! page #'register-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [nav-bar] (.getElementById js/document "navbar"))
  (reagent/render [current-page app-state] (.getElementById js/document "app"))
  (reagent/render [footer] (.getElementById js/document "footer"))
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
