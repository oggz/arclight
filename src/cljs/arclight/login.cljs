(ns arclight.login
  (:require [arclight.util :as util]
            [ajax.core :refer [GET POST]]))

(defn handle-login [app-state response]
  (swap! app-state assoc :logged true)
  (prn response))

(defn handle-logout [app-state response]
  (swap! app-state assoc :logged false)
  (prn response))

(defn get-secured-button [app-state]
  (if (get @app-state :logged)
    [:button.wide {:on-click
                   #(GET "/t2" {:params {:foo 3}
                                :handler
                                (fn [{x :rand}]
                                  (swap! app-state assoc :counter x))})} "GET secured!"]))

(defn login-form [app-state]
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
     [:button.wide
      {:on-click #(POST "/login" {:params {:user user :pass pass}
                                  :handler (partial handle-login app-state)})} "Log in!"]
     [:br]
     [:button.wide
      {:on-click #(GET "/logout" {:params {}
                                  :handler (partial handle-logout app-state)})} "Log out!"]
     [:br]
     [get-secured-button app-state]]))

(defn login-page [app-state]
  [:div
   [:div.alpha
    [:fieldset
     [:legend "Info"]
     [:p "Login is currently fully implemented; however, because registration is not yet fully implemented this page is mostly useless."]]]
   [:div.alpha
    [login-form app-state]]])
