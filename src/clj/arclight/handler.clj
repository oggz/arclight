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
;; (def projects (db/create-table-ddl :projects [[:id :serial "PRIMARY KEY"]
;;                                               [:name "VARCHAR(256)"]
;;                                               [:filename "VARCHAR(256)"]]))
;; (db/execute! db (db/drop-table-ddl :users))
;; (db/execute! db (db/drop-table-ddl :projects))

;; (db/execute! db [users])
;; (db/execute! db [projects])

;; (try
;;   (db/insert! db :users {:userid 10
;;                          :uname "foo"
;;                          :pword "barsasdasdasdklaksdjadskjadslkjasdkjalskjdalksjdlkasjd"})
;;   (catch Exception e
;;     (prn (.getMessage e))))


;; (db/query db ["select * from users"])
;; (db/query db ["select * from projects"])
;; (db/query db ["select * from users where uname = ?" "foobar"])
;; (db/db-do-prepared db ["insert into users (uname, pword) values (?, ?)"
;;                        ["foo" "bar"]
;;                        ["baz" "bazel"]] {:multi? true})
;; (db/db-do-prepared db ["insert into projects (name, filename) values (?, ?)"
;;                        ["default" "default/default.cljs"]
;;                        ["3-Axis CNC - Phase 1" "/phase1/phase1.html"]
;;                        ["3-Axis CNC - Phase 2" "/phase2/phase2.html"]
;;                        ["LM386 Audio Amplifier" "/lm386/lm386.cljs"]]
;;                    {:multi? true})
;; (db/update! db :projects {:name "LM386 Audio Amplifier"} ["name = ?" "lm386"])
;; (db/update! db :projects {:name "3-Axis CNC - Phase 1"} ["name = ?" "CNC Build - Mechanical"])
;; (db/delete! db :projects ["name = ?" "Test Math Render"])
;; (db/insert! db :projects {:name "Math Example" :filename "math_test.html"})
;; (db/insert! db :projects {:name "3-Axis CNC - Phase 2" :filename "phase2/phase2.html"})
;; (db/delete! db :users ["uname = ?" "foobar"])

(def nav-bar
  [:div#navbar])

(def footer
  [:div#footer])

(def mount-target
  [:div#app
      [:h3 "Loading..."]])

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
    [:body {:class "body-container" :background "../images/superstructure.png"}
     nav-bar
     mount-target
     footer
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
        (status 200 (str user " is NOT authentic!")))
      (status 200 (str "Error: " user " was not found in the database!")))))

(defn register-user [request]
  (let [user (get-in request [:params :user])
        pass (get-in request [:params :pass])
        hash (bh/derive pass)]
    ;; (if (nil? (try
    ;;             (db/insert! db :users {:uname user :pword hash})
    ;;             (catch Exception e
    ;;               (prn "ERROR: "(.getMessage e)))))
    ;;   (status 401 (str "Error: " user " was NOT added to the database!"))
    ;;   (status 200 (str user " was added to the database.")))
    (status 200 "Message: Registration is not yet fully implemented!")))

(defn logout-user [req]
  (-> req
      (assoc-in [:cookies "access-token"]  {:value "" :max-age 0})
      (assoc :body {:msg "Logout successful!"})))

(defn foo [req]
  (assoc req :body {:rand (rand-int 100)}))

;; TODO: Blacklist of malicious user names.
(defn restrict
  [handler]
  (fn [req]
    (let [token (get-in req [:cookies "access-token" :value])
          claims (try (jwt/unsign token "secret")
                      (catch Exception _))]
      (if claims
        (-> req (assoc :id claims) handler (dissoc :id))
        (status 401 "Unauthorized!")))))

(defn send-project-data [req]
  (let [project (get-in req [:params :file])
        file (apply :filename (db/query db ["select filename from projects where name = ?" project]))
        filetype (if (re-matches #".*cljs" file) :cljs :html)
        data (if (= filetype :cljs)
               (read-string (slurp (str "resources/public/projects/" file)))
               (slurp (str "resources/public/projects/" file)))
        list (filter (partial not= "default")
                     (into [] (map :name (db/query db ["select name from projects order by id"]))))]
    (-> {}
        (assoc-in [:body :type] filetype)
        (assoc-in [:body :list] list)
        (assoc-in [:body :data] data))))

(defroutes routes
  (GET "/" [] (loading-page))
  (GET "/about" [] (loading-page))
  (GET "/projects" [] (loading-page))
  (GET "/project-data" req (send-project-data req))
  (GET "/calc" [] (loading-page))
  (GET "/register" [] (loading-page))
  (POST "/register" req (register-user req))
  (GET "/login" [] (loading-page))
  (POST "/login" req (handle-login req))
  (GET "/logout" req (logout-user req))
  (GET "/t2" [] (restrict foo))
  (resources "/")
  (not-found "404: The pa1ge you seek was not found!"))

(def app (-> #'routes
             (wrap-defaults site-defaults)
             wrap-restful-format
             wrap-middleware))
