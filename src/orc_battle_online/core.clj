(ns orc_battle_online.core
  (:use ring.adapter.jetty)
  (:use ring.handler.dump)
  (:use (ring.middleware flash reload stacktrace keyword-params params session))
  (:use (ring.middleware.session memory))
  (:use ring.util.response)
  (:use (hiccup core form-helpers))
  (:use (orc_battle_online game_logic html_rendering stateful_links)))

(defmulti handler :uri)

(defmethod handler "/main" [req]
	   (response-html (str (render-game-html req)
			       (create-link "Random Fun"
					    (fn [req]
					      (response-html "Congrats! You have called a method"))))))

(defmethod handler "/newgame" [req]
	   (init-monsters)
	   (init-player)
	   (redirect "/main"))

(defmethod handler "/" [req]
	   (response-html (html [:h1 "Hello World from Ring and Hiccup!"]
				[:a {:href "newgame"} "New Game"])))

(defmethod handler :default [req]
	   (follow-link req))

(def app (-> handler
	     (wrap-flash)
	     (wrap-session {:cookie-attrs {:path "/"} :store (memory-store)})
	     (wrap-keyword-params)
	     (wrap-params)
	     (wrap-reload '(orc_battle_online.core))
	     (wrap-stacktrace)))

(defn boot []
  (init-monsters)
  (init-player)
  (run-jetty app {:port 8000}))

(defn -main [&args]
  (boot))