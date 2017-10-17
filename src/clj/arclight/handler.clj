(ns arclight.handler
  (:require [clojure.pprint :refer [pprint]]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [arclight.middleware :refer [wrap-middleware]]
            [config.core :refer [env]]
            [ring.util.response :refer [response]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [clojure.java.jdbc :as db]
            [buddy.hashers :as bh]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer (wrap-authentication)]
            [buddy.sign.jwe :as jwe]
            [buddy.sign.jwt :as jwt]
            [buddy.core.keys :as keys]
            [buddy.auth.accessrules :as bar]
            ))

;; (jwt/unsign "ayJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjoiZm9vIn0.2oxYJmjU8eqHCCMpMG5u1eTZtTqj-rs3Tm2YAsrZMJc" "secret")

(def db {:dbtype "postgresql"
         :dbname "arclight"
         :user "arclight"
         :password "password"})

;; (def users (db/create-table-ddl :users [[:userid :serial "PRIMARY KEY"]
;;                                         [:uname "VARCHAR(32)"]
;;                                         [:pword "VARCHAR(256)"]]))
;; (db/execute! db (db/drop-table-ddl :users))

;; (db/execute! db [users])

;; (try
;;   (db/insert! db :users {:userid 10
;;                          :uname "foo"
;;                          :pword "barsasdasdasdklaksdjadskjadslkjasdkjalskjdalksjdlkasjd"})
;;   (catch Exception e
;;     (prn (.getMessage e))))


;; (db/query db ["select * from users"])
;; (db/query db ["select * from users where uname = ?" "foobar"])
;; (db/db-do-prepared db ["insert into users (uname, pword) values (?, ?)"
;;                        ["foo" "bar"]
;;                        ["baz" "bazel"]] {:multi? true})

;; (db/delete! db :users ["uname = ?" "baz"])

(def nav-bar
  [:div#navbar])

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   [:title "Arclight"]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container" :background "../images/impeller.jpg"}
     nav-bar
     mount-target
     (anti-forgery-field)
     (include-js "/js/app.js")]))

(defn status [code msg]
  {:status code :body {:msg msg}})

(defn handle-login
  [req]
  (let [user (get-in req [:params :user])
        pass (get-in req [:params :pass])
        umap (first (db/query db ["select * from users where uname = ?" user]))]
    (if (= user (:uname umap))
      (if (bh/check pass (:pword umap))
        (-> req
            (assoc :cookies {"access-token" (jwt/sign {:user (:userid umap)} "secret")})
            (assoc-in [:body] {:msg "Login successful!"}))
        (status 401 (str user " is NOT authentic!")))
      (status 401 (str "Error: " user " was not found in the database!")))))

(defn register-user [request]
  (let [user (get-in request [:params :user])
        pass (get-in request [:params :pass])
        hash (bh/derive pass)]
    (if (nil? (try
                (db/insert! db :users {:uname user :pword hash})
                (catch Exception e
                  (prn "ERROR: "(.getMessage e)))))
      (status 401 (str "Error: " user " was NOT added to the database!"))
      (status 401 (str user " was added to the database.")))))

(defn logout-user [req]
  (-> req
      (assoc-in [:cookies "access-token"]  {:value "" :max-age 0})
      (assoc :body {:msg "Logout successful!"})))

(defn foo [req]
  (assoc req :body {:rand (rand-int 100)}))

(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/register" [] (loading-page))
  (POST "/register" req (register-user req))
  (GET "/login" [] (loading-page))
  (POST "/login" req (handle-login req))
  (GET "/logout" req (logout-user req))
  (GET "/t2" [] (restrict foo))
  (resources "/")
  (not-found "404: The pa1ge you seek was not found!"))

(defn restrict
  [handler]
  (fn [req]
    (let [token (get-in req [:cookies "access-token" :value])
          claims (try (jwt/unsign token "secret")
                      (catch Exception _))]
      (if claims
        (-> req (assoc :id claims) handler (dissoc :id))
        (status 401 "Unauthorized!")))))

(def app (-> #'routes
             (wrap-defaults site-defaults)
             wrap-restful-format
             wrap-middleware))
