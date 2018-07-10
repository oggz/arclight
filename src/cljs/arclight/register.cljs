(ns arclight.register
  (:require [arclight.util :as util]
            [ajax.core :refer [GET POST]]))

  (def info "Registration is currently a work in progress. The database is in place and the mechanisms are mostly there; however, since this is a potential vector of attack, it will be implemented in the live version only after thorough testing.")

(defn handle-register [app-state]
  (fn [response]
    (swap! app-state assoc :error (:msg response))))

(defn post-credentials [app-state user pass email]
  (POST "/register" {:params {:user user :pass pass :email email}
                     :handler (handle-register app-state)}))
(defn show-error [app-state]
  (if (not= "" (:error @app-state))
       [:span
        [:p (:error @app-state)]
        [:button {:on-click #(swap! app-state assoc :error "")} "Ok"]]))

(defn register-form [app-state]
  (let [user (:user @app-state)
        pass (:pass @app-state)
        email (:email @app-state)]
    [:fieldset
     [:legend "Register"]
     [show-error app-state]
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
      {:on-click #(post-credentials app-state user pass email)} "Register"]]))

(defn register-page [app-state]
  [:div
   [:div.alpha
    [:fieldset
     [:legend "Info"]
     [:p info]]]
   [:div.alpha
    [register-form app-state]]])
