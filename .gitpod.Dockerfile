FROM gitpod/workspace-full

RUN brew install clojure/tools/clojure
RUN brew install leiningen
RUN brew install borkdude/brew/babashka
