import React, { useEffect, useState } from "react";
import "./AdminCategoriesPage.css";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { Button, Card, ListGroup } from "react-bootstrap";
import { BsCollection } from "react-icons/bs";
import * as categoriesConstants from "../../../constants/categoriesConstants";
import Loader from "../../../components/Loader";
import Message from "../../../components/Message";
import RoleSidebar from "../../../components/RoleSidebar";
import {
  deleteCategory,
  fetchCategories,
} from "../../../actions/categoriesActions";
import swal from "sweetalert";

const AdminCategoriesPage = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const categoriesReducer = useSelector((state) => state.categoriesReducer);
  const [categories, setCategories] = useState(categoriesReducer.categories);

  const categoryClickHandler = (catId) => {
    navigate(`/adminQuizzes/?catId=${catId}`);
  };

  const addNewCategoryHandler = () => {
    navigate("/adminAddCategory");
  };

  const updateCategoryHandler = (event, category) => {
    event.stopPropagation();
    navigate(`/adminUpdateCategory/${category.catId}/`);
  };

  const deleteCategoryHandler = (event, category) => {
    event.stopPropagation();
    swal({
      title: "Are you sure?",
      text: "Once deleted, you will not be able to recover this category!",
      icon: "warning",
      buttons: true,
      dangerMode: true,
    }).then((willDelete) => {
      if (willDelete) {
        deleteCategory(dispatch, category.catId, token).then((data) => {
          if (data.type === categoriesConstants.DELETE_CATEGORY_SUCCESS) {
            swal(
              "Category Deleted!",
              `${category.title} succesfully deleted`,
              "success"
            );
          } else {
            swal(
              "Category Not Deleted!",
              `${category.title} not deleted`,
              "error"
            );
          }
        });
      } else {
        swal(`${category.title} is safe`);
      }
    });
  };

  useEffect(() => {
    if (!localStorage.getItem("jwtToken")) navigate("/");
  }, []);

  useEffect(() => {
    if (categories.length === 0) {
      fetchCategories(dispatch, token).then((data) => {
        setCategories(data.payload);
      });
    }
  }, []);

  return (
    <div className="adminCategoriesPage__container">
      <div className="adminCategoriesPage__sidebar">
        <RoleSidebar />
      </div>
      <div className="mt-page">
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <h2 style={{ color: "var(--mt-primary)" }}>Classes</h2>
          <Button variant="primary" onClick={addNewCategoryHandler}>
            Add Class
          </Button>
        </div>
        {categories ? (
          categories.length === 0 ? (
            <Message>
              No categories are present. Try adding some categories.
            </Message>
          ) : (
            <Card className="mt-card mt-3">
              <ListGroup variant="flush">
                {categories.map((cat, index) => (
                  <ListGroup.Item
                    key={index}
                    className="d-flex"
                    action
                    onClick={() => categoryClickHandler(cat.catId)}
                  >
                    <div className="ms-2 me-auto d-flex align-items-start">
                      <BsCollection
                        className="mt-1 me-2 flex-shrink-0"
                        style={{ color: "var(--mt-secondary)" }}
                      />
                      <div>
                        <div className="fw-bold">{cat.title}</div>
                        {cat.description}
                      </div>
                    </div>

                    <div
                      style={{
                        display: "flex",
                        height: "90%",
                        margin: "auto 2px",
                      }}
                    >
                      <div
                        onClick={(event) => updateCategoryHandler(event, cat)}
                        style={{
                          margin: "2px 8px",
                          textAlign: "center",
                          color: "#1E7A6F",
                          fontWeight: "500",
                          cursor:"pointer"
                        }}
                      >{`Update`}</div>

                      <div
                        onClick={(event) => deleteCategoryHandler(event, cat)}
                        style={{
                          margin: "2px 8px",
                          textAlign: "center",
                          color: "red",
                          fontWeight: "500",
                          cursor:"pointer"
                        }}
                      >{`Delete`}</div>
                    </div>
                  </ListGroup.Item>
                ))}
              </ListGroup>
            </Card>
          )
        ) : (
          <Loader />
        )}
      </div>
    </div>
  );
};

export default AdminCategoriesPage;
