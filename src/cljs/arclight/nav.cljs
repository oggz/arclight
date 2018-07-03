(ns arclight.nav)

(defn nav-bar [app-state]
  [:nav.header
   ;; (let [w (+ 50 (.-innerWidth js/window))]
   ;;   [:img {:src "/images/mario_run.gif"
   ;;          :style {:height "3em" :bottom -10
   ;;                  :left (- (mod (* 6 (:n @app-state)) (- w 10)) 25)}}])
   [:a.nav {:href "/"} "Arclight Labs"]
   [:a.nav {:href "/about"} "About"]
   [:a.nav {:href "/projects"} "Projects"]
   [:a.nav {:href "/calc"} "BTC Calculator"]
   [:a.nav {:href "/register"} "Register"]
   [:a.nav {:href "/login"} "Login"]
   (if (:logged @app-state)
     [:a.nav {:href "/"} (str "Logged In: " (:user @app-state))])])

(defn footer [app-state]
  [:div.footer
   [:a.nav {:href "https://github.com/oggz/arclight"} "Github"]
   [:a.nav {:href "https://linkedin.com/in/adoccolo"} "LinkedIn"]])
