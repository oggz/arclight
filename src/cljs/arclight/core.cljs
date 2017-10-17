(ns arclight.core
  (:require [cljs.pprint :refer [pprint]]
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
                          :counter 0}))
;; @app-state

(defn post
  [path params handler]
  (POST path {:format :json
              :keywords? true
              :response-format :json
              :params params
              :handler handler
              :error-handler #(log "ERROR: AJAX Handler: " %)}))

(defn ajax-get
  ([path params]
   (ajax-get path params #(log "Handler: "(str %))))

  ([path params handler]
   (GET path {:params params
              :handler handler})))

;; (post "/login"
;;       {:user "foo" :pass "bar"})

;; (GET "/t2" {:params {:foo 3}
;;             :handler #(log "Handler: "(str %))})


;; -------------------------
;; Views


(defn home-page []
  [:div
   [:h2 "Welcome to Arclight... " (:counter @app-state)]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]
   [:div
    [:p (repeat 5 lorem-ipsum)]]])

(defn handle-login [response]
  (if-let [token (:token response)]
    (do
      (swap! app-state assoc :token token)
      (prn response))
    (prn response)))

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
     [:button {:on-click #(post "/login" {:user user :pass pass} handle-login)} "Log in!"]
     [:button {:on-click #(ajax-get "/logout" {})} "Log out!"]
     [:button {:on-click #(ajax-get "/t2" {:foo 3} (fn [{x :rand}] (swap! app-state assoc :counter x)))} "GET secured!"]]))

(defn handle-register [response]
  (prn response))

(defn register-form []
  (let [user (:user @app-state)
        pass (:pass @app-state)]
    [:ul
     [:li [:input {:type "text"
                   :value user
                   :on-change #(swap! app-state assoc :user (.. % -target -value))}]]
     [:li [:input {:type "password"
                   :value pass
                   :on-change #(swap! app-state assoc :pass (.. % -target -value))}]]
     [:button {:on-click #(post "/register" {:user user :pass pass} handle-register)} "POST!"]]))

(defn about-page []
  [:div
   [:h2 "About Arclight... " (:counter @app-state)]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]
   [:h3 "Form Example: "]
   [login-form]])

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
   [:a {:href "/"} "Home"]
   [:a {:href "/about"} "About"]
   [:a {:href "/test"} "Test"]
   [:a {:href "/register"} "Register"]
   [:a {:href "/login"} "Login"]])

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
