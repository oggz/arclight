(ns arclight.register
  (:require [arclight.util :refer [lorem-ipsum]]
            [ajax.core :refer [GET POST]]))

(defn handle-register [response]
  (prn response))

(defn register-form [app-state]
  (let [user (:user @app-state)
        pass (:pass @app-state)
        email (:email @app-state)]
    [:fieldset
     [:legend "Register"]
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

(defn register-page [app-state]
  [:div
   [:div.alpha
    [:fieldset
     [:legend "Info"]
     [:p (take 2000 lorem-ipsum)]]]
   [:div.alpha
    [register-form app-state]]])
