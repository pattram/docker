FROM locustio/locust
ENV LOGLEVEL=INFO 
COPY *.csv /home/locust/
COPY *.py /home/locust/


# ENV PATH="/opt/venv/bin:$PATH"
# # turn off python output buffering
# ENV PYTHONUNBUFFERED=1
# #RUN useradd --create-home locust
# # ensure correct permissions
# #RUN chown -R locust /opt/venv
# USER locust
# WORKDIR /home/locust
# EXPOSE 8089 5557
# #ENTRYPOINT ["locust"]
#ENTRYPOINT ["/opt/venv/bin/locust", "--loglevel", "${LOGLEVEL}"]
#ENTRYPOINT /opt/venv/bin/locust --loglevel ${LOGLEVEL}
#ENTRYPOINT ["/bin/sleep", "1000"]
ENTRYPOINT ["locust", "--loglevel", "DEBUG"] 
