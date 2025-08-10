import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserGameweekListComponent } from './user-gameweek-list.component';

describe('UserGameweekListComponent', () => {
  let component: UserGameweekListComponent;
  let fixture: ComponentFixture<UserGameweekListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserGameweekListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserGameweekListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
