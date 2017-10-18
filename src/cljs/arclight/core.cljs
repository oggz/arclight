(ns arclight.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [chan <! >! timeout]]
            [reagent.core :as reagent :refer [atom]]
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
                          :n 0}))


(defonce go-increment (go-loop []
                        (<! (timeout 50))
                        (swap! app-state update :n inc)
                        (recur)))


;; -------------------------
;; Views

(defn home-page []
  [:div
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
    [:ul
     [:li [:input {:type "text"
                   :value user
                   :on-change #(swap! app-state assoc :user (.. % -target -value))}]]
     [:li [:input {:type "password"
                   :value pass
                   :on-change #(swap! app-state assoc :pass (.. % -target -value))}]]
     [:button {:on-click #(POST "/login" {:params {:user user :pass pass}
                                          :handler handle-login})} "Log in!"]
     [:button {:on-click #(GET "/logout" {:params {}
                                          :handler handle-logout})} "Log out!"]
     [:button {:on-click
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
    [:ul
     [:li [:input {:type "text"
                   :value user
                   :on-change #(swap! app-state assoc :user (.. % -target -value))}]]
     [:li [:input {:type "text"
                   :value email
                   :on-change #(swap! app-state assoc :email (.. % -target -value))}]]
     [:li [:input {:type "password"
                   :value pass
                   :on-change #(swap! app-state assoc :pass (.. % -target -value))}]]
     [:button
      {:on-click
       #(POST "/register" {:params {:user user :pass pass :email email}
                           :handler handle-register})} "POST!"]]))

(defn about-page []
  [:div
   [:h2 "About Arclight... " (:counter @app-state)]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]])

(defn test-page []
  [:div [:h2 "Test... " (:counter @app-state)]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]])

(defn register-page []
  [:div [:h2 "Register New User: "]
   [register-form ]])

(defn login-page []
  [:div
   [:h2 "Log in please: " (:counter @app-state)]
   [login-form]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]])

(defn nav-bar []
  [:nav
   (let [w (.-innerWidth js/window)]
     [:img {:src "/images/mario_run.gif"
            :style {:height "3em" :bottom -10
                    :left (- (mod (* 5 (:n @app-state)) (- w 10)) 25)}}])
   [:a {:href "/"} "Home"]
   [:a {:href "/about"} "About"]
   [:a {:href "/test"} "Test"]
   [:a {:href "/register"} "Register"]
   [:a {:href "/login"} "Login"]
   (if (:logged @app-state)
     [:a {:href "/"} (str "    Hello " (:user @app-state))])
   ])

;; -------------------------
;; Routes

(def page (atom #'home-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'home-page))

(secretary/defroute "/about" []
  (reset! page #'about-page))

(secretary/defroute "/test" []
  (reset! page #'test-page))

(secretary/defroute "/login" []
  (reset! page #'login-page))

(secretary/defroute "/register" []
  (reset! page #'register-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [nav-bar] (.getElementById js/document "navbar"))
  (reagent/render [current-page] (.getElementById js/document "app"))
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
