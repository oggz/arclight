(ns arclight.register
  (:require [arclight.util :as util]
            [ajax.core :refer [GET POST]]))

(def info "Registration is currently a work in progress. The database is in place and the mechanisms are mostly there; however, since this is a potential vector of attack it will be implemented in the live version only after thorough testing.")

(defn handle-register [response]
  (prn response)
  (swap! arclight.core/app-state assoc :error (:msg response)))

(defn post-credentials [user pass email]
  (POST "/register" {:params {:user user :pass pass :email email}
                     :handler handle-register}))
(defn show-error []
  (if (not= "" (@arclight.core/app-state :error))
       [:span
        [:p (get @arclight.core/app-state :error)]
        [:button {:on-click #(swap! arclight.core/app-state assoc :error "")} "Ok"]]))

(defn register-form [app-state]
  (let [user (:user @app-state)
        pass (:pass @app-state)
        email (:email @app-state)]
    [:fieldset
     [:legend "Register"]
     [show-error]
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
      ;; {:on-click (post-credentials user pass email)}
      {:on-click #(post-credentials user pass email)} "Register"]]))

(defn register-page [app-state]
  [:div
   [:div.alpha
    [:fieldset
     [:legend "Info"]
     [:p info]]]
   [:div.alpha
    [register-form app-state]]])
