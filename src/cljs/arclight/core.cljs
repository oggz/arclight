(ns arclight.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [arclight.ajax :refer [load-interceptors!]]
              [ajax.core :refer [GET POST]]))

(def lorem-ipsum
  "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?")

(defonce app-state (atom {:text "foobar"
                          :counter 0}))
;; (.-value (.getElementById js/document "__anti-forgery-token" "__anti-forgery-token"))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))

(defn get-elem-val
  [name]
  (.-value
   (.getElementById js/document (str name))))

(defn post
  []
  (let [name (get-elem-val "uname")]
    (POST "/login" {:format :json
                    :keywords? true
                    :response-format :json
                    :params {:foo name
                             :baz "Red John!"}
                    :handler #(if (= "Red John!" (:baz %))
                                (prn "True" %)
                                (prn "False" %))
                    :error-handler #(prn "ERROR: Handler: " (str %))})))

;; (GET "/t2" {:headers {"Accept" "application/edn"}
;;             :handler #(prn "Handler: "(str %))
;;             :error-handler error-handler})


;; -------------------------
;; Views


(defn home-page []
  [:div
   [:h2 "Welcome to Arclight... " (:counter @app-state)]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]
   [:div
    [:p (repeat 5 lorem-ipsum)]]])

(defn about-page []
  [:div
   [:h2 "About Arclight... " (:counter @app-state)]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]
   [:button {:on-click post} "POST!"]
   [:h3 "Form Example: "]
   [:form {:action "/login" :method "POST"}
    ;; (ring.util.anti-forgery/anti-forgery-field)
    [:input {:type "text" :name "uname" :id "uname"}]
    [:input {:type "password" :name "pword"}]
    [:input {:type "submit" :name "submit"}]]])

(defn test-page []
  [:div [:h2 "Test... " (:counter @app-state)]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]])

(defn nav-bar []
  [:nav
   [:a {:href "/"} "Home"]
   [:a {:href "/about"} "About"]
   [:a {:href "/test"} "Test"]])

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
